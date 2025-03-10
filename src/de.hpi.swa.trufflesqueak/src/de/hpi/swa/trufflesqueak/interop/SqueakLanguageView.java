/*
 * Copyright (c) 2021-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.interop;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchUneagerlyNode;

@SuppressWarnings("static-method")
@ExportLibrary(value = InteropLibrary.class, delegateTo = "delegate")
public final class SqueakLanguageView implements TruffleObject {
    protected final Object delegate;

    public SqueakLanguageView(final Object delegate) {
        this.delegate = delegate;
    }

    @ExportMessage
    protected boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    protected Class<? extends TruffleLanguage<?>> getLanguage() {
        return SqueakLanguage.class;
    }

    @ExportMessage
    protected boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    protected Object getMetaObject(@Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
        return classNode.executeLookup(delegate);
    }

    @ExportMessage
    protected Object toDisplayString(@SuppressWarnings("unused") final boolean allowSideEffects,
                    @Cached final LookupMethodByStringNode lookupNode,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode,
                    @Cached final DispatchUneagerlyNode dispatchNode) {
        final ClassObject classObject = classNode.executeLookup(delegate);
        final Object methodObject = lookupNode.executeLookup(classObject, "asString");
        if (methodObject instanceof CompiledCodeObject) {
            return dispatchNode.executeDispatch((CompiledCodeObject) methodObject, new Object[]{delegate}, NilObject.SINGLETON);
        } else {
            return "Unsupported";
        }
    }

    public static Object create(final Object value) {
        assert isPrimitiveOrFromOtherLanguage(value);
        return new SqueakLanguageView(value);
    }

    /*
     * Language views are intended to be used only for primitives and other language values.
     */
    private static boolean isPrimitiveOrFromOtherLanguage(final Object value) {
        final InteropLibrary interop = InteropLibrary.getFactory().getUncached(value);
        try {
            return !interop.hasLanguage(value) || interop.getLanguage(value) != SqueakLanguage.class;
        } catch (final UnsupportedMessageException e) {
            throw shouldNotReachHere(e);
        }
    }
}
