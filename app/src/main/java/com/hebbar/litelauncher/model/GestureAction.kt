package com.hebbar.litelauncher.model

enum class GestureType {
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    DOUBLE_TAP,
    DOUBLE_TAP_SWIPE,
    LONG_PRESS,
    TWO_FINGER_SWIPE_UP,
    TWO_FINGER_SWIPE_DOWN,
    PINCH_IN,
    PINCH_OUT
}

enum class GestureAction {
    OPEN_DRAWER,
    OPEN_NOTIFICATIONS,
    OPEN_QUICK_SETTINGS,
    OPEN_SEARCH,
    OPEN_SETTINGS,
    DEFAULT_HOME_PAGE,
    LOCK_SCREEN,
    DO_NOTHING
}
