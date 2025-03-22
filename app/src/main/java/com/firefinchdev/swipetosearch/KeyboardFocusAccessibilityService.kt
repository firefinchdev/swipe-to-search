package com.firefinchdev.swipetosearch

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class KeyboardFocusAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "KeyboardFocusService"
        private const val TARGET_EDIT_TEXT_ID = "search_src_text"

        // Time to wait after last content change before considering UI "stable"
        private const val STABILITY_DELAY_MS = 0L
    }

    // Cached list of launcher package names
    private val launcherPackageNames = mutableSetOf<String>()

    // To avoid repeatedly trying to focus the same field
    private var lastFocusAttemptTime = 0L

    // Used to track when UI has settled
    private var lastContentChangeTime = 0L
    private var pendingStabilityCheck = false
    private val stabilityHandler = Handler(Looper.getMainLooper())
    private val stabilityRunnable = Runnable { onUiStabilized() }
    private var started = false

    override fun onServiceConnected() {
        Log.d(TAG, "Accessibility service connected")

        val info = serviceInfo
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                          AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                     AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY or
                     AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS

        serviceInfo = info

        // Initialize list of launcher packages
        findLauncherPackages()
    }

    private fun findLauncherPackages() {
        val packageManager = packageManager
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)

        val resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        for (resolveInfo in resolveInfoList) {
            launcherPackageNames.add(resolveInfo.activityInfo.packageName)
            Log.d(TAG, "Found launcher: ${resolveInfo.activityInfo.packageName}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        // Only process events from launcher packages
        if (!isFromLauncher(packageName)) {
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
                started = true
            }
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> {
                started = false
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (started) {
                    return
                }
                // Cancel any pending stability check since window changed
                cancelPendingStabilityCheck()

                // Wait for content to stabilize
                scheduleStabilityCheck()
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (started) {
                    return
                }
                // Each time content changes, reset the stability timer
                scheduleStabilityCheck()
            }
        }
    }

    private fun isFromLauncher(packageName: String): Boolean {
        return launcherPackageNames.contains(packageName)
    }

    /**
     * Schedule a check that will run after the UI has been stable for a while
     */
    private fun scheduleStabilityCheck() {
        lastContentChangeTime = System.currentTimeMillis()

        // Cancel any existing scheduled checks
        cancelPendingStabilityCheck()

        // Schedule a new check
        stabilityHandler.postDelayed(stabilityRunnable, STABILITY_DELAY_MS)
        pendingStabilityCheck = true
    }

    /**
     * Cancel pending stability check if any
     */
    private fun cancelPendingStabilityCheck() {
        if (pendingStabilityCheck) {
            stabilityHandler.removeCallbacks(stabilityRunnable)
            pendingStabilityCheck = false
        }
    }

    /**
     * Called when UI has been stable for a while - good time to check for search field
     */
    private fun onUiStabilized() {
        pendingStabilityCheck = false
        Log.d(TAG, "UI appears to have stabilized, checking for search field")

        // Only try every few seconds at most to avoid being intrusive
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFocusAttemptTime > 300) {
            lastFocusAttemptTime = currentTime
            checkAndFocusSearchField()
        }
    }

    /**
     * Check if search field exists and focus it
     */
    private fun checkAndFocusSearchField() {
//        Thread.sleep(200)
        val rootNode = rootInActiveWindow ?: return

        try {
            // Look for our target EditText by ID
            val searchEditText = findNodeByViewId(rootNode, TARGET_EDIT_TEXT_ID)

            if (searchEditText != null && searchEditText.isVisibleToUser && searchEditText.isEnabled && searchEditText.text.toString().equals("Search", true)) {
                // Get parent info to help debug
                val parentNode = searchEditText.parent
                val parentInfo = if (parentNode != null) {
                    "Parent class: ${parentNode.className}, visible: ${parentNode.isVisibleToUser}"
                } else {
                    Log.d(TAG, "Failed to focus search EditText")
                }

                Log.d(TAG, "Found search EditText ($parentInfo), focusing it")

                // Make absolutely sure this is really the search field by checking its position
                // is in the top area of the screen (most launcher search fields are at the top)
                if (isLikelySearchField(searchEditText) && !locationSameAsLastSearchFieldLocation(searchEditText)) {
                    // Focus the node
                    if (true/*searchEditText.performAction(AccessibilityNodeInfo.ACTION_FOCUS)*/) {
                        Log.d(TAG, "Successfully focused search EditText")

                        // Wait a bit before showing keyboard
//                        Handler(Looper.getMainLooper()).postDelayed({
                            // Try multiple approaches to trigger the keyboard

                            // 1. Focus and click the node
//                            searchEditText.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                            searchEditText.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                            // 2. Use ACTION_SET_SELECTION to trigger the keyboard
                            val args = Bundle()
                            args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                            args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 0)
                            searchEditText.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)

                            Log.d(TAG, "Triggered keyboard using accessibility actions")
//                        }, 200)
                    } else {
                        Log.d(TAG, "Failed to focus search EditText")
                    }
                } else {
                    Log.d(TAG, "Found EditText with target ID but it doesn't appear to be a search field")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling accessibility event", e)
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Additional heuristic to verify this is likely a search field
     * Most launcher search fields are at the top of the screen
     */
    private fun isLikelySearchField(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        // Check if node is in top third of screen and has reasonable width
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels

        val isInTopThird = rect.top < (screenHeight / 3)
        val hasReasonableWidth = rect.width() > (screenWidth / 2)

        return isInTopThird && hasReasonableWidth
    }

    private var lastNodeLocation = Rect()

    private fun locationSameAsLastSearchFieldLocation(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        val lastNodeLocation = this.lastNodeLocation

        this.lastNodeLocation = rect
        return rect == lastNodeLocation
    }

    private fun findNodeByViewId(node: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? {
        // Check if the current node is what we're looking for
        val idString = node.viewIdResourceName ?: ""
        if (idString.endsWith(viewId)) {
            return node
        }

        // Check all child nodes
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i) ?: continue
            try {
                val result = findNodeByViewId(childNode, viewId)
                if (result != null) {
                    return result
                }
            } finally {
                childNode.recycle()
            }
        }

        return null
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }
}