package de.hpi.swa.trufflesqueak.test;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.IfNilCheck;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.IfThenNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.CascadedSend;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelector;

public class TestDecompile extends TestSqueak {
    public void testIfNil() {
        // (1 ifNil: [true]) class
        // pushConstant: 1, dup, pushConstant: nil, send: ==, jumpFalse: 24, pop, pushConstant:
        // true, send: class, pop, returnSelf
        CompiledCodeObject cm = makeMethod(0x76, 0x88, 0x73, 0xc6, 0x99, 0x87, 0x71, 0xc7, 0x87, 0x78);
        SqueakNode[] bytecodeAST = cm.getBytecodeAST();
        assertEquals(bytecodeAST.length, 2);
        assertSame(bytecodeAST[0].getClass(), SendSelector.class);
        assertSame(bytecodeAST[1].getClass(), ReturnReceiverNode.class);
        SendSelector send = (SendSelector) bytecodeAST[0];
        assertSame(send.selector, image.klass);
        assertSame(send.receiverNode.getClass(), IfNilCheck.class);
    }

    public void testIfNotNil() {
        // (1 ifNotNil: [true]) class
        // pushConstant: 1, pushConstant: nil, send: ==, jumpFalse: 23, pushConstant: nil, jumpTo:
        // 24, pushConstant: true, send: class, pop, returnSelf
        CompiledCodeObject cm = makeMethod(0x76, 0x73, 0xc6, 0x99, 0x73, 0x90, 0x71, 0xc7, 0x87, 0x78);
        SqueakNode[] bytecodeAST = cm.getBytecodeAST();
        assertEquals(bytecodeAST.length, 2);
        assertSame(bytecodeAST[0].getClass(), SendSelector.class);
        assertSame(bytecodeAST[1].getClass(), ReturnReceiverNode.class);
        SendSelector send = (SendSelector) bytecodeAST[0];
        assertSame(send.selector, image.klass);
        assertSame(send.receiverNode.getClass(), IfThenNode.class);
    }

    public void testIfNotNilDo() {
        // (1 ifNotNil: [:o | o class]) class
        // pushConstant: 1, storeIntoTemp: 0, pushConstant: nil, send: ==, jumpFalse: 25,
        // pushConstant: nil, jumpTo: 27, pushTemp: 0, send: class, send: class, pop, returnSelf
        CompiledCodeObject cm = makeMethod(0x76, 0x81, 0x40, 0x73, 0xc6, 0x99, 0x73, 0x91, 0x10, 0xc7, 0xc7, 0x87, 0x78);
        SqueakNode[] bytecodeAST = cm.getBytecodeAST();
        assertEquals(bytecodeAST.length, 2);
        assertSame(bytecodeAST[0].getClass(), SendSelector.class);
        assertSame(bytecodeAST[1].getClass(), ReturnReceiverNode.class);
        SendSelector send = (SendSelector) bytecodeAST[0];
        assertSame(send.selector, image.klass);
        assertSame(send.receiverNode.getClass(), IfThenNode.class);
    }

    public void testCascade() {
        // 1 value; size; class
        // pushConstant: 1, dup, send: value, pop, dup, send: size, pop, send: class, pop,
        // returnSelf
        CompiledCodeObject cm = makeMethod(0x76, 0x88, 0xc9, 0x87, 0x88, 0xc2, 0x87, 0xc7, 0x87, 0x78);
        SqueakNode[] bytecodeAST = cm.getBytecodeAST();
        assertEquals(bytecodeAST.length, 2);
        assertSame(bytecodeAST[0].getClass(), CascadedSend.class);
        assertSame(bytecodeAST[1].getClass(), ReturnReceiverNode.class);
        CascadedSend send = (CascadedSend) bytecodeAST[0];
        assertSame(send.selector, image.klass);
        assertSame(send.receiverNode.getClass(), CascadedSend.class);
        send = (CascadedSend) send.receiverNode;
        assertSame(send.selector, image.size_);
        assertSame(send.receiverNode.getClass(), CascadedSend.class);
        send = (CascadedSend) send.receiverNode;
        assertSame(send.selector, image.value);
        assertSame(send.receiverNode.getClass(), ConstantNode.class);
    }
}
