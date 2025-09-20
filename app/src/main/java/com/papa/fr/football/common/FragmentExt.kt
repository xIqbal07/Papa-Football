package com.papa.fr.football.common

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.papa.fr.football.R
import kotlin.reflect.KClass

fun FragmentManager.navigateToFragment(
    containerId: Int = R.id.fragment_container,
    fragmentClass: KClass<out Fragment>,
    args: Bundle? = null
) {
    val tag = fragmentClass.java.name

    val fragment = findFragmentByTag(tag) ?: fragmentClass.java
        .getDeclaredConstructor()
        .newInstance()
        .apply {
            arguments = args
        }

    commit {
        setReorderingAllowed(true)
        replace(containerId, fragment, tag)
        addToBackStack(tag)
    }
}