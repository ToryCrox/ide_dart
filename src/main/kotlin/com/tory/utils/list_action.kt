package com.tory.utils

fun List<() -> Unit>.mergeCalls(): (() -> Unit)? =
    if (isEmpty())
        null
    else {
        { this.forEach { it() } }
    }
