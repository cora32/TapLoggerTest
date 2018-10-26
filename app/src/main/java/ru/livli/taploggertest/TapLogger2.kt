package ru.livli.taploggertest

import android.app.Dialog
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.KeyEvent.*
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
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

    private fun getReflectedActivity(): AppCompatActivity? {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
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
                                        val windowType = getWindowType(it.callback)
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
                oldWindowType = getWindowType(last.callback)
                oldTitle = getTitle(last)
            } else {
                oldWindowType = ""
                oldTitle = null
            }
        }
    }

    private fun getWindowType(callback: Window.Callback): String = when (callback) {
        is Dialog -> {
            DIALOG
        }
        else -> {
            "--- CHECK ME:$callback".error
            ACTIVITY
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
        val list = object : GestureDetector.OnGestureListener {
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
//                "--- onScroll".error
                return false
            }

            override fun onLongPress(p0: MotionEvent?) {
//                "--- onLongPress".error
            }


        }
        Handler(Looper.getMainLooper()).post {
            val detector = GestureDetector(window.context, list)

            val oldCallback = window.callback
            window.callback = object : Window.Callback {
                override fun onActionModeFinished(mode: ActionMode?) {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onActionModeFinished(mode)
                }

                override fun onCreatePanelView(featureId: Int): View? {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onCreatePanelView(featureId)
                }

                override fun onCreatePanelMenu(featureId: Int, menu: Menu?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onCreatePanelMenu(featureId, menu)
                }

                override fun onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onWindowStartingActionMode(callback)
                }

                @RequiresApi(Build.VERSION_CODES.M)
                override fun onWindowStartingActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onWindowStartingActionMode(callback, type)
                }

                override fun onAttachedToWindow() {
                    oldCallback.onAttachedToWindow()
                }

                override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
                    return oldCallback.dispatchGenericMotionEvent(event)
                }

                override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.dispatchPopulateAccessibilityEvent(event)
                }

                override fun dispatchTrackballEvent(event: MotionEvent?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.dispatchTrackballEvent(event)
                }

                override fun dispatchKeyShortcutEvent(event: KeyEvent?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.dispatchKeyShortcutEvent(event)
                }

                override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    when (event?.action) {
                        ACTION_DOWN -> {
                            when (event.keyCode) {
                                KEYCODE_BACK -> {
                                    "--- user pressed BACK button".error
                                }
                                KEYCODE_HOME -> "--- user pressed HOME button".error
                                KEYCODE_APP_SWITCH -> "--- user pressed APP_SWITCH button".error
                                else -> "--- user pressed ${event.keyCode} button".error
                            }
                        }
                    }
                    return oldCallback.dispatchKeyEvent(event)
                }

                override fun onMenuOpened(featureId: Int, menu: Menu?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onMenuOpened(featureId, menu)
                }

                override fun onPanelClosed(featureId: Int, menu: Menu?) {
                    //To change body of created functions use File | Settings | File Templates. return false
                    oldCallback.onPanelClosed(featureId, menu)
                }

                override fun onMenuItemSelected(featureId: Int, item: MenuItem?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onMenuItemSelected(featureId, item)
                }

                override fun onDetachedFromWindow() {
                    oldCallback.onDetachedFromWindow()
                }

                override fun onPreparePanel(featureId: Int, view: View?, menu: Menu?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onPreparePanel(featureId, view, menu)
                }

                override fun onWindowAttributesChanged(attrs: WindowManager.LayoutParams?) {
                    //To change body of created functions use File | Settings | File Templates. return false
                    oldCallback.onWindowAttributesChanged(attrs)
                }

                override fun onWindowFocusChanged(hasFocus: Boolean) {
                    //To change body of created functions use File | Settings | File Templates. return false
                    oldCallback.onWindowFocusChanged(hasFocus)
                }

                override fun onContentChanged() {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onContentChanged()
                }

                override fun onSearchRequested(): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onSearchRequested()
                }

                @RequiresApi(Build.VERSION_CODES.M)
                override fun onSearchRequested(searchEvent: SearchEvent?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false return false
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
    }

    private fun iterateChildren(resources: Resources, window: Window, event: MotionEvent): Boolean {
        window.decorView.childrenRecursiveSequence().forEach {
            if (it.isClickable) {
//                "--- ${it.id}".error
                if (it.id > -1) {
//                    "--- ${activity.resources.getResName(it.id)}".error
                    when (it) {
                        is TextView -> {
                            val x = event.x.toInt()
                            val y = event.y.toInt()
                            it.getGlobalVisibleRect(r)

                            if (r.contains(x, y)) {
                                ("--- \"${it.text}\" ${r.contains(x, y)} ${resources.getResName(it.id)}").error
                                selectedView = WeakReference(it)
                                return true
                            }
                        }
                        is ImageView -> {
                            val x = event.x.toInt()
                            val y = event.y.toInt()
                            it.getGlobalVisibleRect(r)

                            if (r.contains(x, y)) {
                                //get drawable and draw it to canvas here
                                ("--- ${r.contains(x, y)} ${resources.getResName(it.id)}").error
                                selectedView = WeakReference(it)
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
                                ("--- ${label?.text} ${r.contains(x, y)} ${resources.getResName(it.id)}").error
                                selectedView = WeakReference(it)
                                return true
                            }
                        }
                        else -> {
                            "--- clicked: ${it.isClickable} ${it.javaClass} ${resources.getResName(it.id)}".error
                        }
                    }
                }
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