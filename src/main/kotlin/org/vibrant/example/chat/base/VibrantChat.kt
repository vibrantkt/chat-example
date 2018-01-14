package org.vibrant.example.chat.base

import org.vibrant.example.chat.base.node.BaseNode
import org.vibrant.example.chat.base.node.HTTPPeer
import org.vibrant.example.chat.base.producers.BaseBlockChainProducer

class VibrantChat(val node: BaseNode){
    fun start(){
        this.node.start()
    }
}