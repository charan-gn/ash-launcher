package app.ash.launcher.ui

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import app.ash.launcher.MainViewModel
import app.ash.launcher.R
import app.ash.launcher.data.Prefs
import app.ash.launcher.databinding.FragmentHomeBinding
import app.ash.launcher.helper.AsciiAssets
import app.ash.launcher.helper.expandNotificationDrawer
import app.ash.launcher.helper.getColorFromAttr
import app.ash.launcher.player.MusicPlayer
import app.ash.launcher.ui.widget.AsciiClock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class HomeFragment : BaseFragment(), View.OnClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private var pageCount = Prefs.DEFAULT_PAGE_COUNT
    private var startPage = 0
    private var currentPage = 0
    private var pageNames: List<String> = emptyList()
    private lateinit var pagerAdapter: HomePagerAdapter
    private var lastClockKey = ""
    private var permissionAsked = false
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE yyyy-MM-dd", Locale.ENGLISH)
    private val musicIndex: Int get() = pageNames.indexOf(MUSIC_PAGE)
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && ::viewModel.isInitialized) viewModel.musicPlayer.loadTracks()
        if (_binding != null) updateMusicPanel()
    }

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            updateClockAndStatus()
            clockHandler.postDelayed(this, STATUS_REFRESH_MS)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        pageCount = prefs.pageCount
        startPage = (prefs.startPage - 1).coerceIn(0, pageCount - 1)
        pageNames = resources.getStringArray(R.array.home_pages).take(pageCount)
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        initPager()
        preloadPageArt()
        initTabs()
        initObservers()
        viewModel.musicPlayer.onChanged = {
            if (_binding != null) updateMusicPanel()
        }
        binding.setDefaultLauncher.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        viewModel.isOlauncherDefault()
        if (prefs.showStatusBar) showStatusBar()
        else hideStatusBar()
        clockHandler.removeCallbacks(clockTick)
        clockHandler.post(clockTick)
        if (currentPage == musicIndex) ensureMusicReady() else updateMusicPanel()
    }

    override fun onPause() {
        clockHandler.removeCallbacks(clockTick)
        super.onPause()
    }

    override fun onClick(view: View) {
        if (view.id == R.id.setDefaultLauncher)
            viewModel.resetLauncherLiveData.call()
    }

    private fun initPager() {
        pagerAdapter = HomePagerAdapter(
            requireContext(),
            pageNames,
            object : HomePagerAdapter.GestureCallbacks {
                override fun onSwipeUp() {
                    openAppDrawer()
                }

                override fun onSwipeDown() {
                    expandNotificationDrawer(requireContext())
                }

                override fun onLongPress() {
                    openSettings()
                }

                override fun onTap(position: Int, xFraction: Float) {
                    if (pageNames.getOrNull(position) == MUSIC_PAGE) handleMusicTap(xFraction)
                }
            }
        )
        binding.homePager.adapter = pagerAdapter
        binding.homePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                updateTabs(position)
                updateClockAndStatus()
                if (position == musicIndex) ensureMusicReady()
            }
        })
        currentPage = startPage
        binding.homePager.setCurrentItem(startPage, false)
        updateClockAndStatus()
    }

    private fun initTabs() {
        binding.tabBar.removeAllViews()
        val typeface = ResourcesCompat.getFont(requireContext(), R.font.jetbrains_mono_regular)
        val horizontalPadding = (6 * resources.displayMetrics.density).toInt()
        pageNames.forEachIndexed { index, name ->
            val tab = TextView(requireContext()).apply {
                text = " $name "
                textSize = 14f
                letterSpacing = 0f
                this.typeface = typeface
                setPadding(horizontalPadding, 0, horizontalPadding, 0)
                setOnClickListener { binding.homePager.setCurrentItem(index, true) }
            }
            binding.tabBar.addView(tab)
        }
        updateTabs(currentPage)
    }

    private fun updateTabs(selected: Int) {
        val activeBackground = requireContext().getColorFromAttr(R.attr.primaryColor)
        val activeText = requireContext().getColorFromAttr(R.attr.primaryInverseColor)
        val inactiveText = requireContext().getColorFromAttr(R.attr.primaryColorTrans50)
        for (index in 0 until binding.tabBar.childCount) {
            val tab = binding.tabBar.getChildAt(index) as TextView
            if (index == selected) {
                tab.setBackgroundColor(activeBackground)
                tab.setTextColor(activeText)
            } else {
                tab.background = null
                tab.setTextColor(inactiveText)
            }
        }
    }

    private fun preloadPageArt() {
        val staticPages = setOf("notes", "tasks", "more")
        pageNames.forEachIndexed { index, name ->
            if (name !in staticPages) return@forEachIndexed
            val lines = AsciiAssets.lines(requireContext(), name) +
                    listOf("", getString(R.string.page_empty))
            pagerAdapter.setPageContent(index, lines)
        }
        updateMusicPanel()
    }

    private fun ensureMusicReady() {
        if (musicIndex < 0) return
        val context = context ?: return
        if (!MusicPlayer.hasPermission(context)) {
            updateMusicPanel()
            if (!permissionAsked && isAdded) {
                permissionAsked = true
                audioPermissionLauncher.launch(MusicPlayer.audioPermission())
            }
            return
        }
        viewModel.musicPlayer.loadTracks()
        updateMusicPanel()
    }

    private fun updateMusicPanel() {
        val index = musicIndex
        if (index < 0 || !::pagerAdapter.isInitialized) return
        val context = context ?: return
        val art = AsciiAssets.lines(context, MUSIC_PAGE)
        val lines = if (!MusicPlayer.hasPermission(context)) {
            art + listOf("", context.getString(R.string.music_permission))
        } else {
            val snapshot = viewModel.musicPlayer.snapshot()
            when {
                snapshot.loading -> art + listOf("", context.getString(R.string.music_loading))
                snapshot.count == 0 -> art + listOf("", context.getString(R.string.music_no_tracks))
                else -> nowPlayingLines(snapshot)
            }
        }
        pagerAdapter.setPageContent(index, lines)
    }

    private fun nowPlayingLines(snapshot: MusicPlayer.Snapshot): List<String> {
        val title = snapshot.title.ifBlank { getString(R.string.music_unknown) }.take(MAX_TRACK_CHARS)
        val artist = snapshot.artist.ifBlank { getString(R.string.music_unknown) }.take(MAX_TRACK_CHARS)
        val action = if (snapshot.isPlaying) "pause" else " play"
        val ratio = if (snapshot.durationMs > 0) {
            snapshot.positionMs.toFloat() / snapshot.durationMs
        } else {
            0f
        }
        val barWidth = 16
        val filled = (ratio.coerceIn(0f, 1f) * barWidth).roundToInt()
        val bar = "█".repeat(filled) + "░".repeat(barWidth - filled)
        return buildList {
            add(title)
            add(artist)
            add("")
            add("[ << ]  [$action]  [ >> ]")
            add("")
            add("${formatDuration(snapshot.positionMs)} $bar ${formatDuration(snapshot.durationMs)}")
            add("${snapshot.index + 1} / ${snapshot.count}")
            if (snapshot.error) add(getString(R.string.music_error))
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    private fun handleMusicTap(xFraction: Float) {
        if (musicIndex < 0) return
        ensureMusicReady()
        val player = viewModel.musicPlayer
        when {
            xFraction < 0.34f -> player.prev()
            xFraction > 0.66f -> player.next()
            else -> player.playPause()
        }
        updateMusicPanel()
    }

    private fun updateClockAndStatus() {
        val now = Date()
        val time = timeFormat.format(now)
        val date = dateFormat.format(now).lowercase(Locale.ENGLISH)
        val blinkColon = (now.time / 1000) % 2 == 0L
        val name = pageNames.getOrElse(currentPage) { "" }
        binding.statusLine.text = getString(R.string.home_status, time, name)
        binding.hintLine.text = getString(
            if (name == MUSIC_PAGE) R.string.music_hint else R.string.home_hint,
        )
        if (name == MUSIC_PAGE) updateMusicPanel()

        val clockKey = time + blinkColon + date
        if (clockKey == lastClockKey) return
        lastClockKey = clockKey
        val clockTime = if (blinkColon) time else time.replace(":", " ")
        val lines = AsciiClock.lines(clockTime) + listOf("", date)
        binding.homePager.post {
            if (isAdded && _binding != null) pagerAdapter.setHomeContent(lines)
        }
    }

    private fun initObservers() {
        viewModel.isOlauncherDefault.observe(viewLifecycleOwner) { isDefault ->
            binding.setDefaultLauncher.isVisible = isDefault.not() && prefs.hideSetDefaultLauncher.not()
        }
    }

    private fun openAppDrawer() {
        findNavController().navigate(R.id.action_mainFragment_to_appListFragment)
    }

    private fun openSettings() {
        findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
    }

    private fun showStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
        else
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

    override fun onDestroyView() {
        clockHandler.removeCallbacks(clockTick)
        if (::viewModel.isInitialized) viewModel.musicPlayer.onChanged = null
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val STATUS_REFRESH_MS = 1_000L
        const val MAX_TRACK_CHARS = 28
        const val MUSIC_PAGE = "music"
    }
}
