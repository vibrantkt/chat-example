package org.vibrant.example.chat.base

import org.vibrant.example.chat.base.node.Node

class VibrantChat(val node: Node){
    fun start(){
        this.node.start()
    }
}