package dev.jagoba.skinholder.views.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.jagoba.skinholder.R
import dev.jagoba.skinholder.core.GlobalViewModel
import dev.jagoba.skinholder.databinding.CardLastRegistryStatsBinding
import dev.jagoba.skinholder.databinding.CardLatencyStatsBinding
import dev.jagoba.skinholder.databinding.CardVarianceStatsBinding
import dev.jagoba.skinholder.databinding.FragmentHomeBinding
import dev.jagoba.skinholder.dataservice.repository.DashboardRepository
import dev.jagoba.skinholder.models.dashboard.DashboardStats
import dev.jagoba.skinholder.models.dashboard.LastRegistryStats
import dev.jagoba.skinholder.models.dashboard.LatencyStats
import dev.jagoba.skinholder.models.dashboard.VarianceStats
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val globalViewModel: GlobalViewModel by activityViewModels()

    private enum class Platform { STEAM, GAMERPAY, CSFLOAT }

    /** Local UI state for the variance card platform toggle. */
    private var selectedPlatform: Platform = Platform.STEAM

    /** Cached last successful stats so the toggle can re-render without round-tripping the VM. */
    private var lastStats: DashboardStats? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupVariancePlatformToggle()
        binding.btnRetry.setOnClickListener { viewModel.loadDashboard() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    globalViewModel.currentUsername.collect { username ->
                        if (!username.isNullOrBlank()) {
                            val full = getString(R.string.welcome_greeting, username)
                            val spannable = SpannableString(full)
                            val start = full.indexOf(username)
                            spannable.setSpan(
                                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.primary)),
                                start,
                                start + username.length,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            binding.textGreeting.text = spannable
                        } else {
                            binding.textGreeting.text = ""
                        }
                    }
                }
                launch {
                    viewModel.uiState.collect(::renderState)
                }
            }
        }
    }

    private fun renderState(state: DashboardUiState) {
        when (state) {
            is DashboardUiState.Loading -> {
                binding.groupSkeleton.visibility = View.VISIBLE
                binding.groupContent.visibility = View.GONE
                binding.groupError.visibility = View.GONE
            }
            is DashboardUiState.Success -> {
                binding.groupSkeleton.visibility = View.GONE
                binding.groupContent.visibility = View.VISIBLE
                binding.groupError.visibility = View.GONE
                lastStats = state.stats
                bindLastRegistry(binding.includeLastRegistry, state.stats.lastRegistry)
                bindLatency(binding.includeLatency, state.stats.latency)
                bindVariance(binding.includeVariance, state.stats.variance, selectedPlatform)
            }
            is DashboardUiState.Error -> {
                binding.groupSkeleton.visibility = View.GONE
                binding.groupContent.visibility = View.GONE
                binding.groupError.visibility = View.VISIBLE
                binding.textErrorMessage.text = state.message
            }
        }
    }

    // ---------- Last Registry ----------

    private fun bindLastRegistry(b: CardLastRegistryStatsBinding, stats: LastRegistryStats?) {
        if (stats == null) {
            b.textEmpty.visibility = View.VISIBLE
            b.groupData.visibility = View.GONE
            return
        }
        b.textEmpty.visibility = View.GONE
        b.groupData.visibility = View.VISIBLE
        b.textTotalSteam.text = getString(R.string.dashboard_value_eur, stats.totalSteam)
        b.textTotalGamerpay.text = getString(R.string.dashboard_value_eur, stats.totalGamepay)
        b.textTotalCsfloat.text = getString(R.string.dashboard_value_eur, stats.totalCsfloat)
    }

    // ---------- Latency ----------

    private fun bindLatency(b: CardLatencyStatsBinding, stats: LatencyStats) {
        applyLatency(b.textLatencySteam, stats.steam)
        applyLatency(b.textLatencyGamerpay, stats.gamerpay)
        applyLatency(b.textLatencyCsfloat, stats.csfloat)
    }

    private fun applyLatency(view: android.widget.TextView, latencyMs: Long) {
        val ctx = requireContext()
        if (latencyMs == DashboardRepository.PING_FAILED) {
            view.text = getString(R.string.dashboard_na)
            view.setTextColor(ContextCompat.getColor(ctx, R.color.status_neutral))
            return
        }
        view.text = getString(R.string.dashboard_value_ms, latencyMs.toInt())
        val color = when {
            latencyMs < LATENCY_GOOD_MS -> R.color.status_success
            latencyMs < LATENCY_OK_MS -> R.color.status_warning
            else -> R.color.status_error
        }
        view.setTextColor(ContextCompat.getColor(ctx, color))
    }

    // ---------- Variance ----------

    private fun setupVariancePlatformToggle() {
        val card = binding.includeVariance
        card.btnPlatformSteam.setOnClickListener { onPlatformSelected(Platform.STEAM) }
        card.btnPlatformGamerpay.setOnClickListener { onPlatformSelected(Platform.GAMERPAY) }
        card.btnPlatformCsfloat.setOnClickListener { onPlatformSelected(Platform.CSFLOAT) }
        updatePlatformToggleState()
    }

    private fun onPlatformSelected(platform: Platform) {
        if (selectedPlatform == platform) return
        selectedPlatform = platform
        updatePlatformToggleState()
        lastStats?.let { bindVariance(binding.includeVariance, it.variance, selectedPlatform) }
    }

    private fun updatePlatformToggleState() {
        val card = binding.includeVariance
        card.btnPlatformSteam.isSelected = selectedPlatform == Platform.STEAM
        card.btnPlatformGamerpay.isSelected = selectedPlatform == Platform.GAMERPAY
        card.btnPlatformCsfloat.isSelected = selectedPlatform == Platform.CSFLOAT
    }

    private fun bindVariance(
        b: CardVarianceStatsBinding,
        stats: VarianceStats,
        platform: Platform
    ) {
        val (week, month, year) = when (platform) {
            Platform.STEAM -> Triple(
                stats.weeklyVariancePercentSteam,
                stats.monthlyVariancePercentSteam,
                stats.yearlyVariancePercentSteam
            )
            Platform.GAMERPAY -> Triple(
                stats.weeklyVariancePercentGamerPay,
                stats.monthlyVariancePercentGamerPay,
                stats.yearlyVariancePercentGamerPay
            )
            Platform.CSFLOAT -> Triple(
                stats.weeklyVariancePercentCSFloat,
                stats.monthlyVariancePercentCSFloat,
                stats.yearlyVariancePercentCSFloat
            )
        }
        applyVariance(b.iconVarianceWeek, b.textVarianceWeek, week)
        applyVariance(b.iconVarianceMonth, b.textVarianceMonth, month)
        applyVariance(b.iconVarianceYear, b.textVarianceYear, year)
    }

    private fun applyVariance(
        iconView: android.widget.ImageView,
        textView: android.widget.TextView,
        value: Double
    ) {
        val ctx = requireContext()
        if (value == VarianceStats.NA_VALUE) {
            iconView.setImageResource(R.drawable.ic_trending_up)
            val neutral = ContextCompat.getColor(ctx, R.color.status_neutral)
            iconView.imageTintList = android.content.res.ColorStateList.valueOf(neutral)
            textView.text = getString(R.string.dashboard_na)
            textView.setTextColor(neutral)
            return
        }
        val isPositive = value >= 0.0
        val color = ContextCompat.getColor(
            ctx,
            if (isPositive) R.color.status_success else R.color.status_error
        )
        iconView.setImageResource(
            if (isPositive) R.drawable.ic_trending_up else R.drawable.ic_trending_down
        )
        iconView.imageTintList = android.content.res.ColorStateList.valueOf(color)
        textView.text = if (isPositive) {
            getString(R.string.dashboard_variance_format_positive, value)
        } else {
            getString(R.string.dashboard_variance_format_negative, value)
        }
        textView.setTextColor(color)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val LATENCY_GOOD_MS = 100L
        const val LATENCY_OK_MS = 300L
    }
}
