package com.tory.action

import com.tory.action.data.GenerationData
import com.tory.action.data.PerformAction

interface StaticActionProcessor {

    fun processAction(generationData: GenerationData): PerformAction?

}
