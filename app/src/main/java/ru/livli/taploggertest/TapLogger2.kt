package ru.livli.taploggertest

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import org.jetbrains.anko.childrenRecursiveSequence
import java.lang.ref.WeakReference


object TapLogger {
    private const val ACTIVITY = "ACTIVITY"
    private const val DIALOG = "DIALOG"
    private val windowList = ArrayList<Window>()
    private val r = Rect()
    private var selectedView: WeakReference<View>? = null
    private var oldJavaClassName = ""
    private var oldWindowType = ""
    private var oldTitle: String? = null
    private val activityThreadClass = Class.forName("android.app.ActivityThread")
    private val onGestureListener = object : GestureDetector.OnGestureListener {
        override fun onShowPress(p0: MotionEvent?) {
//                "--- onShowPress".error
        }

        override fun onSingleTapUp(p0: MotionEvent?): Boolean {
//                "--- onSingleTapUp".error
//                selectedView?.get()?.performClick()
            return false
        }

        override fun onDown(p0: MotionEvent?): Boolean {
            return false
        }

        override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
            if (Math.abs(p2) > Math.abs(p3)) {
                if (p2 > 0.5f) "--- onFling right".error
                else "--- onFling left".error
            } else {
                if (p3 > 0.5f) "--- onFling down".error
                else "--- onFling up".error
            }

            return false
        }

        override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
            if (Math.abs(p2) > Math.abs(p3)) {
                if (p2 > 0.5f) "--- onScroll left".error
                else "--- onScroll right".error
            } else {
                if (p3 > 0.5f) "--- onScroll up".error
                else "--- onScroll down".error
            }
            return false
        }

        override fun onLongPress(p0: MotionEvent?) {
//                "--- onLongPress".error
        }
    }

    private fun getReflectedActivity(): AppCompatActivity? {
        val activityThread = activityThreadClass.getMethod("currentActivityThread")
                .invoke(null)
        val activitiesField = activityThreadClass.getDeclaredField("mActivities")
        activitiesField.isAccessible = true
        val activities = activitiesField.get(activityThread) as Map<*, *>
        if (activities.isNotEmpty()) {
            val activityClient = activities.iterator().next().value
            activityClient?.let {
                val activityField = activityClient::class.java.getDeclaredField("activity")
                activityField.isAccessible = true
                val act = activityField.get(activityClient) as? AppCompatActivity
                return act
            }
        }
        return null
    }

    fun start() {
        async {
            while (true) {
                val activity = getReflectedActivity()
                activity?.let {
                    val manager = activity.windowManager
                    val globalField = manager.javaClass.getDeclaredField("mGlobal")
                    globalField.isAccessible = true
                    val global = globalField.get(manager)

                    val rootsField = global.javaClass.getDeclaredField("mRoots")
                    rootsField.isAccessible = true
                    val roots = rootsField.get(global) as ArrayList<*>

                    roots.forEach {
                        val windowCallbacksField = it.javaClass.getDeclaredField("mWindowCallbacks")
                        windowCallbacksField.isAccessible = true
                        val windowCallbacks = windowCallbacksField.get(it) as? ArrayList<*>

                        windowCallbacks?.let {
                            windowCallbacks.forEach {
                                val windowField = it.javaClass.getDeclaredField("mWindow")
                                windowField.isAccessible = true
                                val window = windowField.get(it) as? Window

                                window?.let {
                                    if (!windowList.contains(it)) {
                                        windowList.add(window)

                                        val title = getTitle(window).toString()
//                                        "--- ${window.context} ${title} ${window.callback}".error

                                        val javaClassName = window.context.javaClass.name
                                        val windowType = getWindowType(it.context)
                                        if (window.context.javaClass.name == oldJavaClassName) {
                                            "--- user reentered $windowType $title $javaClassName".error
                                        } else {
                                            when (oldWindowType) {
                                                DIALOG -> "--- from $oldWindowType $oldTitle to $windowType $title $javaClassName".error
                                                else -> "--- from $oldWindowType $oldTitle to $windowType $title $javaClassName".error
                                            }
                                        }
                                        oldJavaClassName = javaClassName
                                        oldWindowType = windowType
                                        oldTitle = title

                                        setupListeners(window)
                                    }
                                }

                                if (window == null)
                                    "--- window = null was (${window?.javaClass?.name})".error
                            }
                        }
                    }

                    clearList()
                }
                delay(1000)
            }
        }.invokeOnCompletion {
            if (it != null) {
                "--- ex: $it ${it.localizedMessage}".error
                it.printStackTrace()
            }
        }
    }

    private fun clearList() {
        if (windowList.removeAll {
                    val pred = !ViewCompat.isAttachedToWindow(it.decorView)
                    if (pred) {
                        "--- user left $oldWindowType $oldTitle ${it.context.javaClass.name}".error
                    }
                    pred
                }) {
            if (windowList.isNotEmpty()) {
                val last = windowList.last()
                oldWindowType = getWindowType(last.context)
                oldTitle = getTitle(last)
            } else {
                oldWindowType = ""
                oldTitle = null
            }
        }
    }

    private fun getWindowType(context: Context): String =
            when (context) {
                is Dialog -> DIALOG
                is Activity -> ACTIVITY
                is ContextThemeWrapper -> DIALOG
                else -> {
                    "UNKNOWN ${context.javaClass.name}"
                }
            }

    private fun getTitle(window: Window): String? =
            try {
                val titleField = window.javaClass.getDeclaredField("mTitle")
                titleField.isAccessible = true
                titleField.get(window) as? String
            } catch (ex: Exception) {
                null
            }

    private fun setupListeners(window: Window) {
        (window.context as? AppCompatActivity)?.supportFragmentManager?.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
                super.onFragmentAttached(fm, f, context)
                "--- user entered fragment ${f.tag}".error
//                f.activity?.window?.let {
//                    Handler(Looper.getMainLooper()).post {
//                        it.callback = getWindowCallback(it, it.callback, GestureDetector(window.context, onGestureListener))
//                    }
//                }
            }

            override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
                super.onFragmentDetached(fm, f)
                "--- user left fragment ${f.tag}".error
            }
        }, true)

        Handler(Looper.getMainLooper()).post {
            window.callback = getWindowCallback(window, window.callback, GestureDetector(window.context, onGestureListener))
        }
    }

    private fun getWindowCallback(window: Window, oldCallback: Window.Callback?, detector: GestureDetector): Window.Callback? {
        oldCallback?.let {
            return object : Window.Callback {
                override fun onActionModeFinished(mode: ActionMode?) {
                    return oldCallback.onActionModeFinished(mode)
                }

                override fun onCreatePanelView(featureId: Int): View? {
                    return oldCallback.onCreatePanelView(featureId)
                }

                override fun onCreatePanelMenu(featureId: Int, menu: Menu?): Boolean {
                    return oldCallback.onCreatePanelMenu(featureId, menu)
                }

                override fun onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? {
                    return oldCallback.onWindowStartingActionMode(callback)
                }

                @RequiresApi(Build.VERSION_CODES.M)
                override fun onWindowStartingActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
                    return oldCallback.onWindowStartingActionMode(callback, type)
                }

                override fun onAttachedToWindow() {
                    oldCallback.onAttachedToWindow()
                }

                override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
                    return oldCallback.dispatchGenericMotionEvent(event)
                }

                override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean {
                    return oldCallback.dispatchPopulateAccessibilityEvent(event)
                }

                override fun dispatchTrackballEvent(event: MotionEvent?): Boolean {
                    return oldCallback.dispatchTrackballEvent(event)
                }

                override fun dispatchKeyShortcutEvent(event: KeyEvent?): Boolean {
                    return oldCallback.dispatchKeyShortcutEvent(event)
                }

                override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                    when (event?.action) {
                        KeyEvent.ACTION_DOWN -> {
                            when (event.keyCode) {
                                KeyEvent.KEYCODE_BACK -> {
                                    "--- user pressed BACK button".error
                                }
                                KeyEvent.KEYCODE_HOME -> "--- user pressed HOME button".error
                                KeyEvent.KEYCODE_APP_SWITCH -> "--- user pressed APP_SWITCH button".error
                                else -> "--- user pressed ${event.keyCode} button".error
                            }
                        }
                    }
                    return oldCallback.dispatchKeyEvent(event)
                }

                override fun onMenuOpened(featureId: Int, menu: Menu?): Boolean {
                    return oldCallback.onMenuOpened(featureId, menu)
                }

                override fun onPanelClosed(featureId: Int, menu: Menu?) {
                    oldCallback.onPanelClosed(featureId, menu)
                }

                override fun onMenuItemSelected(featureId: Int, item: MenuItem?): Boolean {
                    return oldCallback.onMenuItemSelected(featureId, item)
                }

                override fun onDetachedFromWindow() {
                    oldCallback.onDetachedFromWindow()
                }

                override fun onPreparePanel(featureId: Int, view: View?, menu: Menu?): Boolean {
                    return oldCallback.onPreparePanel(featureId, view, menu)
                }

                override fun onWindowAttributesChanged(attrs: WindowManager.LayoutParams?) {
                    oldCallback.onWindowAttributesChanged(attrs)
                }

                override fun onWindowFocusChanged(hasFocus: Boolean) {
                    oldCallback.onWindowFocusChanged(hasFocus)
                }

                override fun onContentChanged() {
                    return oldCallback.onContentChanged()
                }

                override fun onSearchRequested(): Boolean {
                    return oldCallback.onSearchRequested()
                }

                @RequiresApi(Build.VERSION_CODES.M)
                override fun onSearchRequested(searchEvent: SearchEvent?): Boolean {
                    return oldCallback.onSearchRequested(searchEvent)
                }

                override fun onActionModeStarted(mode: ActionMode?) {
                    oldCallback.onActionModeStarted(mode)
                }

                override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
                    when (event?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            if (!iterateChildren(window.context.resources, window, event))
                                "--- unresponsive?".error
                        }
                    }

                    detector.onTouchEvent(event)
                    return oldCallback.dispatchTouchEvent(event)
                }

            }
        }

        return null
    }

    private fun iterateChildren(resources: Resources, window: Window, event: MotionEvent): Boolean {
        window.decorView.childrenRecursiveSequence().forEach {
            if (it.isClickable) {
                if (it.id > -1) {
                    when (it) {
                        is TextView -> {
                            val x = event.x.toInt()
                            val y = event.y.toInt()
                            it.getGlobalVisibleRect(r)

                            if (r.contains(x, y)) {
                                ("--- clicked: \"${it.text}\" ${resources.getResName(it.id)}").error
//                                selectedView = WeakReference(it)
                                selectedView = null
                                return true
                            }
                        }
                        is ImageView -> {
                            val x = event.x.toInt()
                            val y = event.y.toInt()
                            it.getGlobalVisibleRect(r)

                            if (r.contains(x, y)) {
                                //get drawable and draw it to canvas here
                                ("--- clicked: ${resources.getResName(it.id)}").error
//                                selectedView = WeakReference(it)
                                selectedView = null
                                return true
                            }
                        }
                        is BottomNavigationItemView -> {
                            val x = event.x.toInt()
                            val y = event.y.toInt()
                            it.getGlobalVisibleRect(r)

                            if (r.contains(x, y)) {
                                val labelField = it::class.java.getDeclaredField("largeLabel")
                                labelField.isAccessible = true
                                val label = labelField.get(it) as? TextView
                                ("--- clicked: ${label?.text} ${resources.getResName(it.id)}").error
//                                selectedView = WeakReference(it)
                                selectedView = null
                                return true
                            }
                        }
                        else -> {
                            if (it.hasOnClickListeners()) {
                                selectedView = WeakReference(it)
                                "--- last clickable: ${it.hasOnClickListeners()} ${it.javaClass.name} ${resources.getResName(it.id)}".error
//                                val x = event.x.toInt()
//                                val y = event.y.toInt()
//                                it.getGlobalVisibleRect(r)
//
//                                if (r.contains(x, y)) {
//                                    ("--- clicked: ${resources.getResName(it.id)}").error
////                                    selectedView = WeakReference(it)
//                                    return true
//                                }
                            } else {
                                "--- child: ${it.hasOnClickListeners()} ${it.javaClass.name} ${resources.getResName(it.id)}".error
                            }
                        }
                    }
                }
            }
        }

        selectedView?.get()?.let {
            val x = event.x.toInt()
            val y = event.y.toInt()
            it.getGlobalVisibleRect(r)

            if (r.contains(x, y)) {
                //get drawable and draw it to canvas here
                ("--- clicked lastClickable: ${resources.getResName(it.id)}").error
//                                selectedView = WeakReference(it)
                selectedView = null
                return true
            }
        }
        return false
    }

    private fun Resources.getResName(id: Int): String? =
            try {
                getResourceName(id)
            } catch (ex: Exception) {
                null
            }
}