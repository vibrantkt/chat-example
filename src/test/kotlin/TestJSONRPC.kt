import org.junit.Assert.assertEquals
import org.junit.Test
import org.vibrant.example.chat.base.BaseJSONSerializer
import org.vibrant.base.rpc.json.JSONRPCRequest
import org.vibrant.base.rpc.json.JSONRPCResponse

import org.vibrant.example.chat.base.util.serialize

class TestJSONRPC {


    @Test
    fun `JSON RPC response serialization`(){
        val request = JSONRPCResponse(
                result = "Hood",
                error = null,
                id = 1
        )

        val serialized = request.serialize()
        assertEquals(
                "{\"result\":\"Hood\",\"error\":null,\"id\":1,\"version\":\"2.0\"}",
                serialized
        )
    }

    @Test
    fun `JSON RPC response deserialization`(){
        val request = JSONRPCResponse(
                result = "Hood",
                error = null,
                id = 1
        )

        val serialized = BaseJSONSerializer.serialize(request)
        val deserialized = BaseJSONSerializer.deserializeJSONRPC(serialized)
        assertEquals(
                request,
                deserialized
        )
    }

    @Test
    fun `JSON RPC request serialization`(){
        val request = JSONRPCRequest(
                "callWithNoParams",
                arrayOf(),
                1
        )

        val serialized = request.serialize()
        assertEquals(
                "{\"method\":\"callWithNoParams\",\"params\":[],\"id\":1,\"version\":\"2.0\"}",
                serialized
        )
    }

    @Test
    fun `JSON RPC request deserialization`(){
        val request = JSONRPCRequest(
                "callWithNoParams",
                arrayOf(),
                1
        )

        val serialized = BaseJSONSerializer.serialize(request)
        val deserialized = BaseJSONSerializer.deserializeJSONRPC(serialized)

        assertEquals(
                request,
                deserialized
        )
    }


    @Test
    fun `JSON RPC request invocation`(){
//        val request = JSONRPCRequest(
//                "echo",
//                arrayOf("Hello"),
//                1
//        )
//
//        val vibrant = VibrantChat(Node(vibrant))
//        val fakeProtocol = BaseJSONRPCProtocol(Node())
//
//
//
//
//        val some = fakeProtocol.invoke(request, RemoteNode("localhost", 1234))
//        assertEquals(
//                JSONRPCResponse(result="Hello", error=null, id=1, version="2.0"),
//                some
//        )


    }



}