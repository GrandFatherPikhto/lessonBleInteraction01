package com.grandfatherpikhto.lessonbleinteraction01

import android.view.View

typealias clickHandler<T> = (T, View) -> Unit
typealias longClickHandler<T> = (T, View) -> Unit