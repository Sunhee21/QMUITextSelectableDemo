package com.example.newtextdemo

import java.util.regex.Pattern

/**
 * @intro
 * @author sunhee
 * @date 2019/11/20
 */
fun main(args: Array<String>) {
    val url = "http://www.shandw.com/mi/game/2039051215.html?channel=12582"
    val pattern = Pattern.compile("\\d+")
    val m = pattern.matcher(url)
    if (m.find())println("${m.group()}")
}