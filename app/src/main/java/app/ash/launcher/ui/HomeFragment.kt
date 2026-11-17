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
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import app.ash.launcher.MainViewModel
import app.ash.launcher.R
import app.ash.launcher.data.Prefs
import app.ash.launcher.databinding.FragmentHomeBinding
import app.ash.launcher.helper.expandNotificationDrawer
import app.ash.launcher.helper.getColorFromAttr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : BaseFragment(), View.OnClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private var pageCount = Prefs.DEFAULT_PAGE_COUNT
    private var startPage = 0
    private var currentPage = 0
    private var pageNames: List<String> = emptyList()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            updateStatusLine()
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
        initTabs()
        initObservers()
        binding.setDefaultLauncher.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        binding.homePager.setCurrentItem(startPage, false)
        viewModel.isOlauncherDefault()
        if (prefs.showStatusBar) showStatusBar()
        else hideStatusBar()
        clockHandler.removeCallbacks(clockTick)
        clockHandler.post(clockTick)
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
        binding.homePager.adapter = HomePagerAdapter(
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
            }
        )
        binding.homePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                updateTabs(position)
                updateStatusLine()
            }
        })
        currentPage = startPage
        binding.homePager.setCurrentItem(startPage, false)
        updateStatusLine()
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

    private fun updateStatusLine() {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val name = pageNames.getOrElse(currentPage) { "" }
        binding.statusLine.text = getString(R.string.home_status, time, name)
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
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val STATUS_REFRESH_MS = 30_000L
    }
}
