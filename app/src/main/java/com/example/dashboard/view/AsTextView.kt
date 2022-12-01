package com.example.dashboard.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.example.dashboard.R

class AsTextView : AppCompatTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setGradeText(grade: String) {
        this.visibility = VISIBLE
        when {
            grade > "0" -> {
                this.setTextColor(ResourcesCompat.getColor(resources, R.color.progressGood, null))
                this.setText(R.string.good)
            }
            grade > "1" -> {
                this.setTextColor(ResourcesCompat.getColor(resources, R.color.progressNormal, null))
                this.setText(R.string.normal)
            }
            grade > "2" -> {
                this.setText(R.string.bad)
                this.setTextColor(ResourcesCompat.getColor(resources, R.color.progressBad, null))
            }
            grade > "3" -> {
                this.setText(R.string.very_bad)
                this.setTextColor(ResourcesCompat.getColor(resources, R.color.progressWorst, null))
            }

            else ->  {
                this.setTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.statusUnitText,
                        null
                    )
                )
                this.setText(R.string.error)
            }
        }
    }
}