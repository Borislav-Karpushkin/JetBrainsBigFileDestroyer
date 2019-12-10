package com.example.demo.app

import javafx.scene.text.FontWeight
import tornadofx.Stylesheet
import tornadofx.box
import tornadofx.cssclass
import tornadofx.px

class Styles : Stylesheet() {

    companion object {
        val heading by cssclass()
    }

    init {
        label and heading {
            padding = box(3.px)
            fontSize = 14.px
            fontWeight = FontWeight.BOLD
        }
    }
}