/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.image;

import java.io.PrintWriter;
import java.lang.ref.ReferenceQueue;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.graalvm.nativeimage.Platform;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.TruffleLanguage.ParsingRequest;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.Message;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.Source;

import de.hpi.swa.trufflesqueak.SqueakImage;
import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.SqueakOptions.SqueakContextOptions;
import de.hpi.swa.trufflesqueak.aot.SqueakSDL2Display;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.interop.LookupMethodByStringNode;
import de.hpi.swa.trufflesqueak.io.SqueakDisplay;
import de.hpi.swa.trufflesqueak.io.SqueakDisplayInterface;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.FRACTION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.MESSAGE;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.POINT;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.model.layout.SlotLocation;
import de.hpi.swa.trufflesqueak.nodes.DoItRootNode;
import de.hpi.swa.trufflesqueak.nodes.ExecuteTopLevelContextNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.MiscellaneousBytecodes.CallPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsState;
import de.hpi.swa.trufflesqueak.nodes.plugins.B2D;
import de.hpi.swa.trufflesqueak.nodes.plugins.BitBlt;
import de.hpi.swa.trufflesqueak.nodes.plugins.JPEGReader;
import de.hpi.swa.trufflesqueak.nodes.plugins.Zip;
import de.hpi.swa.trufflesqueak.shared.SqueakImageLocator;
import de.hpi.swa.trufflesqueak.tools.SqueakMessageInterceptor;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.MethodCacheEntry;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class SqueakImageContext {
    private static final ContextReference<SqueakImageContext> REFERENCE = ContextReference.create(SqueakLanguage.class);
    private static final String PREPARE_HEADLESS_IMAGE_SCRIPT = "PrepareHeadlessImage.st";

    /* Special objects */
    public final ClassObject trueClass = new ClassObject(this);
    public final ClassObject falseClass = new ClassObject(this);
    public final PointersObject schedulerAssociation = new PointersObject(this);
    public final ClassObject bitmapClass = new ClassObject(this);
    public final ClassObject smallIntegerClass = new ClassObject(this);
    public final ClassObject byteStringClass = new ClassObject(this);
    public final ClassObject arrayClass = new ClassObject(this);
    public final PointersObject smalltalk = new PointersObject(this);
    public final ClassObject floatClass = new ClassObject(this);
    public final ClassObject methodContextClass = new ClassObject(this);
    public final ClassObject pointClass = new ClassObject(this);
    public final ClassObject largePositiveIntegerClass = new ClassObject(this);
    public final ClassObject messageClass = new ClassObject(this);
    public final ClassObject compiledMethodClass = new ClassObject(this);
    public final ClassObject semaphoreClass = new ClassObject(this);
    public final ClassObject characterClass = new ClassObject(this);
    public final NativeObject doesNotUnderstand = new NativeObject(this);
    public final NativeObject cannotReturn = new NativeObject(this);
    public final NativeObject mustBeBooleanSelector = new NativeObject(this);
    public final ClassObject byteArrayClass = new ClassObject(this);
    public final ClassObject processClass = new ClassObject(this);
    public final ClassObject blockClosureClass = new ClassObject(this);
    public final ClassObject largeNegativeIntegerClass = new ClassObject(this);
    public final NativeObject aboutToReturnSelector = new NativeObject(this);
    public final NativeObject runWithInSelector = new NativeObject(this);
    public final ArrayObject primitiveErrorTable = new ArrayObject(this);
    public final ArrayObject specialSelectors = new ArrayObject(this);
    @CompilationFinal public ClassObject fullBlockClosureClass;
    @CompilationFinal public ClassObject smallFloatClass;
    @CompilationFinal private ClassObject byteSymbolClass;
    @CompilationFinal private ClassObject foreignObjectClass;
    @CompilationFinal private ClassObject linkedListClass;

    public final ArrayObject specialObjectsArray = new ArrayObject(this);
    public final ClassObject metaClass = new ClassObject(this);
    public final ClassObject nilClass = new ClassObject(this);

    public final CompiledCodeObject dummyMethod = new CompiledCodeObject(this, null, new Object[]{CompiledCodeObject.makeHeader(true, 1, 0, 0, false, true)}, compiledMethodClass);

    /* Method Cache */
    private static final int METHOD_CACHE_SIZE = 2 << 12;
    private static final int METHOD_CACHE_MASK = METHOD_CACHE_SIZE - 1;
    private static final int METHOD_CACHE_REPROBES = 4;
    private int methodCacheRandomish;
    @CompilationFinal(dimensions = 1) private final MethodCacheEntry[] methodCache = new MethodCacheEntry[METHOD_CACHE_SIZE];

    /* System Information */
    public final SqueakImageFlags flags = new SqueakImageFlags();
    private String imagePath;
    @CompilationFinal public int imageFormat = 0;
    private final TruffleFile homePath;
    @CompilationFinal(dimensions = 1) private byte[] resourcesDirectoryBytes;
    @CompilationFinal(dimensions = 1) private byte[] resourcesPathBytes;
    private final boolean isHeadless;
    public final SqueakContextOptions options;
    private final SqueakSystemAttributes systemAttributes = new SqueakSystemAttributes(this);

    /* System */
    public NativeObject clipboardTextHeadless = asByteString("");
    private boolean currentMarkingFlag;
    private ArrayObject hiddenRoots;
    private long globalClassCounter = -1;
    @CompilationFinal private SqueakDisplayInterface display;
    public final CheckForInterruptsState interrupt;
    public final long startUpMillis = System.currentTimeMillis();
    public final ReferenceQueue<AbstractSqueakObject> weakPointersQueue = new ReferenceQueue<>();

    /* Truffle */
    private final AllocationReporter allocationReporter;
    @CompilationFinal public SqueakLanguage.Env env;
    private final SqueakLanguage language;
    private Source lastParseRequestSource;
    @CompilationFinal private PrintWriter output;
    @CompilationFinal private PrintWriter error;
    private final HashMap<Message, NativeObject> interopMessageToSelectorMap = new HashMap<>();

    @CompilationFinal private SqueakImage squeakImage;

    /* Stack Management */
    private ContextObject interopExceptionThrowingContextPrototype;
    public ContextObject lastSeenContext;

    @CompilationFinal private ClassObject exceptionClass;
    @CompilationFinal private ClassObject fractionClass;
    private PointersObject parserSharedInstance;
    private AbstractSqueakObject requestorSharedInstanceOrNil;
    @CompilationFinal private PointersObject scheduler;
    @CompilationFinal private Object smalltalkScope;
    @CompilationFinal private ClassObject wideStringClass;

    /* Plugins */
    public final B2D b2d = new B2D(this);
    public final BitBlt bitblt = new BitBlt(this);
    public String[] dropPluginFileList = new String[0];
    public final JPEGReader jpegReader = new JPEGReader();
    public final Zip zip = new Zip();

    /* Error detection for headless execution */
    @CompilationFinal(dimensions = 1) public static final byte[] DEBUG_ERROR_SELECTOR_NAME = "debugError:".getBytes();
    @CompilationFinal private NativeObject debugErrorSelector;
    @CompilationFinal(dimensions = 1) public static final byte[] DEBUG_SYNTAX_ERROR_SELECTOR_NAME = "debugSyntaxError:".getBytes();
    @CompilationFinal private NativeObject debugSyntaxErrorSelector;

    public SqueakImageContext(final SqueakLanguage squeakLanguage, final SqueakLanguage.Env environment) {
        language = squeakLanguage;
        patch(environment);
        options = new SqueakContextOptions(env);
        isHeadless = options.isHeadless;
        interrupt = new CheckForInterruptsState(this);
        allocationReporter = env.lookup(AllocationReporter.class);
        SqueakMessageInterceptor.enableIfRequested(environment);
        final String truffleLanguageHome = language.getTruffleLanguageHome();
        if (truffleLanguageHome != null) {
            homePath = env.getInternalTruffleFile(truffleLanguageHome);
        } else { /* Fall back to image directory if language home is not set. */
            homePath = env.getInternalTruffleFile(options.imagePath).getParent();
        }
        assert homePath.exists() : "Home directory does not exist: " + homePath;
        initializeMethodCache();
    }

    public static SqueakImageContext get(final Node node) {
        return REFERENCE.get(node);
    }

    public static SqueakImageContext getSlow() {
        CompilerAsserts.neverPartOfCompilation();
        return get(null);
    }

    public void ensureLoaded() {
        if (squeakImage == null) {
            // Load image.
            SqueakImageReader.load(this);
            if (options.disableStartup) {
                printToStdOut("Skipping startup routine...");
                return;
            }

            final String prepareHeadlessImageScript = MiscUtils.getStringResource(getClass(), PREPARE_HEADLESS_IMAGE_SCRIPT);
            if (prepareHeadlessImageScript == null) {
                printToStdErr("Unable to find " + PREPARE_HEADLESS_IMAGE_SCRIPT);
                return;
            }
            try {
                evaluate(prepareHeadlessImageScript);
            } catch (final Exception e) {
                printToStdErr("startUpList failed:");
                printToStdErr(e);
            }
        }
    }

    public SqueakImage getSqueakImage() {
        if (squeakImage == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            squeakImage = new SqueakImage(this);
        }
        return squeakImage;
    }

    @TruffleBoundary
    public Object evaluate(final String sourceCode) {
        return getDoItContextNode(sourceCode, false).getCallTarget().call();
    }

    @TruffleBoundary
    public Object lookup(final String member) {
        return smalltalk.send(this, "at:ifAbsent:", asByteSymbol(member), NilObject.SINGLETON);
    }

    public boolean patch(final SqueakLanguage.Env newEnv) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        env = newEnv;
        output = new PrintWriter(env.out(), true);
        error = new PrintWriter(env.err(), true);
        return true;
    }

    @TruffleBoundary
    public ExecuteTopLevelContextNode getActiveContextNode() {
        final PointersObject activeProcess = getActiveProcessSlow();
        final ContextObject activeContext = (ContextObject) activeProcess.instVarAt0Slow(PROCESS.SUSPENDED_CONTEXT);
        activeProcess.instVarAtPut0Slow(PROCESS.SUSPENDED_CONTEXT, NilObject.SINGLETON);
        return ExecuteTopLevelContextNode.create(this, getLanguage(), activeContext, true);
    }

    @TruffleBoundary
    public DoItRootNode getDoItContextNode(final ParsingRequest request) {
        final Source source = request.getSource();
        lastParseRequestSource = source;
        final String sourceCode;
        if (isFileInFormat(source)) {
            sourceCode = String.format("[[ (FileStream readOnlyFileNamed: '%s') fileIn. true ] on: Error do: [ :e | Interop throwException: e ]]", source.getPath());
        } else {
            if (request.getArgumentNames().isEmpty()) {
                sourceCode = String.format("[ %s ]", source.getCharacters().toString());
            } else {
                sourceCode = String.format("[ :%s | %s ]", String.join(" :", request.getArgumentNames()), source.getCharacters().toString());
            }
        }
        return new DoItRootNode(this, language, evaluate(sourceCode));
    }

    private static boolean isFileInFormat(final Source source) {
        final CharSequence firstLine = source.getCharacters(1);
        /* First line must end with an `!`. */
        return firstLine.charAt(firstLine.length() - 1) == '!';
    }

    @TruffleBoundary
    private ExecuteTopLevelContextNode getDoItContextNode(final String source, final boolean isExternalRequest) {
        /*
         * (Parser new parse: '1 + 2 * 3' class: UndefinedObject noPattern: true notifying: nil
         * ifFail: [^nil]) generate
         */

        if (parserSharedInstance == null) {
            parserSharedInstance = (PointersObject) ((ClassObject) lookup("Parser")).send(this, "new");
            final Object polyglotRequestorClassOrNil = lookup("PolyglotRequestor");
            if (polyglotRequestorClassOrNil instanceof ClassObject) {
                requestorSharedInstanceOrNil = (AbstractSqueakObject) ((ClassObject) polyglotRequestorClassOrNil).send(this, "default");
            } else {
                requestorSharedInstanceOrNil = NilObject.SINGLETON;
            }
        }

        final NativeObject smalltalkSource = asByteString(source);
        if (requestorSharedInstanceOrNil != NilObject.SINGLETON) {
            ((AbstractSqueakObjectWithClassAndHash) requestorSharedInstanceOrNil).send(this, "currentSource:", smalltalkSource);
        }
        final PointersObject methodNode;
        try {
            methodNode = (PointersObject) parserSharedInstance.send(this, "parse:class:noPattern:notifying:ifFail:",
                            smalltalkSource, nilClass, BooleanObject.TRUE, requestorSharedInstanceOrNil, BlockClosureObject.create(this, blockClosureClass, 0));
        } catch (final ProcessSwitch e) {
            /*
             * A ProcessSwitch exception is thrown in case of a syntax error to open the
             * corresponding window. Fail with an appropriate exception here. This way, it is clear
             * why code execution failed (e.g. when requested through the Polyglot API).
             */
            throw CompilerDirectives.shouldNotReachHere("Unexpected process switch detected during parse request", e);
        }
        final CompiledCodeObject doItMethod = (CompiledCodeObject) methodNode.send(this, "generate");

        final ContextObject doItContext = ContextObject.create(this, doItMethod.getSqueakContextSize());
        doItContext.setReceiver(NilObject.SINGLETON);
        doItContext.setCodeObject(doItMethod);
        doItContext.setInstructionPointer(doItMethod.getInitialPC());
        doItContext.setStackPointer(doItMethod.getNumTemps());
        doItContext.setSenderUnsafe(isExternalRequest ? getInteropExceptionThrowingContext() : NilObject.SINGLETON);
        return ExecuteTopLevelContextNode.create(this, getLanguage(), doItContext, false);
    }

    /**
     * Returns a fake context for BlockClosure>>#on:do: that handles any exception (and may rethrow
     * it as Interop exception). This allows Smalltalk exceptions to be thrown to other languages,
     * so that they can catch them. The mechanism works essentially like this:
     *
     * <pre>
     * <code>[ ... ] on: Exception do: [ :e | "handle e" ]</code>
     * </pre>
     *
     * (see Context>>#handleSignal:)
     */
    public ContextObject getInteropExceptionThrowingContext() {
        if (interopExceptionThrowingContextPrototype == null) {
            assert evaluate("Interop") != NilObject.SINGLETON : "Interop class must be present";
            final CompiledCodeObject onDoMethod = (CompiledCodeObject) evaluate("BlockClosure>>#on:do:");
            interopExceptionThrowingContextPrototype = ContextObject.create(this, onDoMethod.getSqueakContextSize());
            interopExceptionThrowingContextPrototype.setCodeObject(onDoMethod);
            interopExceptionThrowingContextPrototype.setReceiver(NilObject.SINGLETON);
            /*
             * Need to catch all exceptions here. Otherwise, the contexts sender is used to find the
             * next handler context (see Context>>#nextHandlerContext).
             */
            interopExceptionThrowingContextPrototype.atTempPut(0, evaluate("Exception"));
            /*
             * Throw Error and Halt as interop, ignore warnings, handle all other exceptions the
             * usual way via UndefinedObject>>#handleSignal:.
             */
            interopExceptionThrowingContextPrototype.atTempPut(1, evaluate(
                            "[ :e | ((e isKindOf: Error) or: [ e isKindOf: Halt ]) ifTrue: [ Interop throwException: e \"rethrow as interop\" ] ifFalse: [(e isKindOf: Warning) ifTrue: [ e resume \"ignore\" ] " +
                                            "ifFalse: [ nil handleSignal: e \"handle the usual way\" ] ] ]"));
            interopExceptionThrowingContextPrototype.atTempPut(2, BooleanObject.TRUE);
            interopExceptionThrowingContextPrototype.setInstructionPointer(onDoMethod.getInitialPC() + CallPrimitiveNode.NUM_BYTECODES);
            interopExceptionThrowingContextPrototype.setStackPointer(onDoMethod.getNumTemps());
            interopExceptionThrowingContextPrototype.removeSender();
        }
        return interopExceptionThrowingContextPrototype.shallowCopy();
    }

    /*
     * ACCESSING
     */

    public PrintWriter getOutput() {
        return output;
    }

    public PrintWriter getError() {
        return error;
    }

    public SqueakLanguage getLanguage() {
        return language;
    }

    public boolean getCurrentMarkingFlag() {
        return currentMarkingFlag;
    }

    public boolean toggleCurrentMarkingFlag() {
        return currentMarkingFlag = !currentMarkingFlag;
    }

    public ArrayObject getHiddenRoots() {
        return hiddenRoots;
    }

    public TruffleFile getHomePath() {
        return homePath;
    }

    public NativeObject getResourcesDirectory() {
        ensureResourcesDirectoryAndPathInitialized();
        return NativeObject.newNativeBytes(this, byteStringClass, resourcesDirectoryBytes.clone());
    }

    public NativeObject getResourcesPath() {
        ensureResourcesDirectoryAndPathInitialized();
        return NativeObject.newNativeBytes(this, byteStringClass, resourcesPathBytes.clone());
    }

    private void ensureResourcesDirectoryAndPathInitialized() {
        if (resourcesDirectoryBytes == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final String languageHome = getLanguage().getTruffleLanguageHome();
            final TruffleFile path;
            if (languageHome != null) {
                path = getHomePath().resolve("resources");
            } else { /* Fallback to image directory. */
                path = env.getInternalTruffleFile(getImagePath()).getParent();
                if (path == null) {
                    throw SqueakException.create("`parent` should not be `null`.");
                }
            }
            resourcesDirectoryBytes = MiscUtils.stringToBytes(path.getAbsoluteFile().getPath());
            resourcesPathBytes = MiscUtils.stringToBytes(path.getAbsoluteFile().getPath() + env.getFileNameSeparator());
        }
    }

    public long getGlobalClassCounter() {
        return globalClassCounter;
    }

    public void setGlobalClassCounter(final long newValue) {
        assert globalClassCounter < 0 : "globalClassCounter should only be set once";
        globalClassCounter = newValue;
    }

    public long getNextClassHash() {
        return ++globalClassCounter;
    }

    public NativeObject getDebugErrorSelector() {
        return debugErrorSelector;
    }

    public void setDebugErrorSelector(final NativeObject nativeObject) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert debugErrorSelector == null;
        debugErrorSelector = nativeObject;
    }

    public NativeObject getDebugSyntaxErrorSelector() {
        return debugSyntaxErrorSelector;
    }

    public void setDebugSyntaxErrorSelector(final NativeObject nativeObject) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert debugSyntaxErrorSelector == null;
        debugSyntaxErrorSelector = nativeObject;
    }

    public ClassObject getExceptionClass() {
        if (exceptionClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            exceptionClass = (ClassObject) evaluate("Exception");
        }
        return exceptionClass;
    }

    public ClassObject getByteSymbolClass() {
        return byteSymbolClass;
    }

    public void setByteSymbolClass(final ClassObject classObject) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert byteSymbolClass == null;
        byteSymbolClass = classObject;
    }

    public ClassObject getWideStringClass() {
        if (wideStringClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            // TODO: find a better way to find wideStringClass or do this on image side instead?
            final CompiledCodeObject method = (CompiledCodeObject) LookupMethodByStringNode.getUncached().executeLookup(byteArrayClass, "asWideString");
            if (method != null) {
                final PointersObject assoc = (PointersObject) method.getLiteral(1);
                wideStringClass = (ClassObject) assoc.instVarAt0Slow(ASSOCIATION.VALUE);
            } else {
                /* Image only uses a single String class (e.g. Cuis 5.0). */
                wideStringClass = byteStringClass;
            }
        }
        return wideStringClass;
    }

    public static void initializeBeforeLoadingImage() {
        SlotLocation.initialize();
    }

    public void initializeAfterLoadingImage(final ArrayObject theHiddenRoots) {
        assert hiddenRoots == null;
        hiddenRoots = theHiddenRoots;
    }

    public ClassObject getForeignObjectClass() {
        assert foreignObjectClass != null;
        return foreignObjectClass;
    }

    public boolean setForeignObjectClass(final ClassObject classObject) {
        if (foreignObjectClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            foreignObjectClass = classObject;
            return true;
        } else {
            return false;
        }
    }

    public ClassObject getLinkedListClass() {
        if (linkedListClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final Object lists = getScheduler().instVarAt0Slow(PROCESS_SCHEDULER.PROCESS_LISTS);
            linkedListClass = SqueakObjectClassNode.getUncached().executeLookup(((ArrayObject) lists).getObject(0));
        }
        return linkedListClass;
    }

    public boolean supportsNFI() {
        CompilerAsserts.neverPartOfCompilation();
        return env.getInternalLanguages().containsKey("nfi");
    }

    public PointersObject getScheduler() {
        if (scheduler == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            scheduler = (PointersObject) schedulerAssociation.instVarAt0Slow(ASSOCIATION.VALUE);
        }
        return scheduler;
    }

    public PointersObject getActiveProcessSlow() {
        return AbstractPointersObjectReadNode.getUncached().executePointers(getScheduler(), PROCESS_SCHEDULER.ACTIVE_PROCESS);
    }

    public Object getSpecialObject(final int index) {
        return specialObjectsArray.getObjectStorage()[index];
    }

    public void setSpecialObject(final int index, final Object value) {
        specialObjectsArray.getObjectStorage()[index] = value;
    }

    private ArrayObject getSpecialSelectors() {
        return (ArrayObject) getSpecialObject(SPECIAL_OBJECT.SPECIAL_SELECTORS);
    }

    public NativeObject getSpecialSelector(final int index) {
        return (NativeObject) getSpecialSelectors().getObjectStorage()[index * 2];
    }

    public int getSpecialSelectorNumArgs(final int index) {
        return MiscUtils.toIntExact((long) getSpecialSelectors().getObjectStorage()[index * 2 + 1]);
    }

    public void setSemaphore(final int index, final AbstractSqueakObject semaphore) {
        assert semaphore == NilObject.SINGLETON || isSemaphoreClass(((AbstractSqueakObjectWithClassAndHash) semaphore).getSqueakClass());
        setSpecialObject(index, semaphore);
    }

    public boolean hasDisplay() {
        return display != null;
    }

    public SqueakDisplayInterface getDisplay() {
        return display;
    }

    public String getImagePath() {
        if (imagePath == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setImagePath(SqueakImageLocator.findImage(options.imagePath));
        }
        return imagePath;
    }

    public void setImagePath(final String path) {
        imagePath = path;
    }

    @TruffleBoundary
    public String getImageDirectory() {
        final Path parent = Paths.get(getImagePath()).getParent();
        if (parent != null) {
            return parent.toString();
        } else {
            throw SqueakException.create("Could not determine image directory.");
        }
    }

    public String[] getImageArguments() {
        if (options.imageArguments.length > 0) {
            return options.imageArguments;
        } else {
            return env.getApplicationArguments();
        }
    }

    public AbstractSqueakObject getSystemAttribute(final int index) {
        return systemAttributes.getSystemAttribute(index);
    }

    public Source getLastParseRequestSource() {
        return lastParseRequestSource;
    }

    public boolean interruptHandlerDisabled() {
        return options.disableInterruptHandler;
    }

    public boolean isHeadless() {
        return isHeadless;
    }

    public void attachDisplayIfNecessary() {
        if (!isHeadless) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (TruffleOptions.AOT) {
                /* SDL2 backend only supported on Linux and Darwin */
                if (Platform.includedIn(Platform.LINUX.class) || Platform.includedIn(Platform.DARWIN.class)) {
                    display = new SqueakSDL2Display(this);
                }
            } else {
                display = new SqueakDisplay(this);
            }
        }
    }

    public boolean isTesting() {
        return options.isTesting;
    }

    public void finalizeContext() {
        if (options.printResourceSummary) {
            MiscUtils.printResourceSummary(this);
        }
    }

    @TruffleBoundary
    public Object getScope() {
        ensureLoaded();
        if (smalltalkScope == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            smalltalkScope = smalltalk.send(this, "asInteropScope");
        }
        return smalltalkScope;
    }

    /*
     * METHOD CACHE
     */

    private void initializeMethodCache() {
        for (int i = 0; i < METHOD_CACHE_SIZE; i++) {
            methodCache[i] = new MethodCacheEntry();
        }
    }

    /*
     * Probe the cache, and return the matching entry if found. Otherwise return one that can be
     * used (selector and class set) with method == null. Initial probe is class xor selector,
     * reprobe delta is selector. We do not try to optimize probe time -- all are equally 'fast'
     * compared to lookup. Instead we randomize the reprobe so two or three very active conflicting
     * entries will not keep dislodging each other.
     */
    @ExplodeLoop
    public MethodCacheEntry findMethodCacheEntry(final ClassObject classObject, final NativeObject selector) {
        methodCacheRandomish = methodCacheRandomish + 1 & 3;
        final int selectorHash = System.identityHashCode(selector);
        int firstProbe = (System.identityHashCode(classObject) ^ selectorHash) & METHOD_CACHE_MASK;
        int probe = firstProbe;
        for (int i = 0; i < METHOD_CACHE_REPROBES; i++) {
            final MethodCacheEntry entry = methodCache[probe];
            if (entry.getClassObject() == classObject && entry.getSelector() == selector) {
                return entry;
            }
            if (i == methodCacheRandomish) {
                firstProbe = probe;
            }
            probe = probe + selectorHash & METHOD_CACHE_MASK;
        }
        return methodCache[firstProbe].reuseFor(classObject, selector);
    }

    /* Clear all cache entries (prim 89). */
    public void flushMethodCache() {
        for (int i = 0; i < METHOD_CACHE_SIZE; i++) {
            methodCache[i].freeAndRelease();
        }
    }

    /* Clear cache entries for selector (prim 119). */
    public void flushMethodCacheForSelector(final NativeObject selector) {
        for (int i = 0; i < METHOD_CACHE_SIZE; i++) {
            if (methodCache[i].getSelector() == selector) {
                methodCache[i].freeAndRelease();
            }
        }
    }

    /* Clear cache entries for method (prim 116). */
    public void flushMethodCacheForMethod(final CompiledCodeObject method) {
        for (int i = 0; i < METHOD_CACHE_SIZE; i++) {
            if (methodCache[i].getResult() == method) {
                methodCache[i].freeAndRelease();
            }
        }
    }

    public void flushMethodCacheAfterBecome() {
        /* TODO: Could be selective by checking class, selector, and method against mutations. */
        flushMethodCache();
    }

    /*
     * CLASS CHECKS
     */

    public boolean isBitmapClass(final ClassObject object) {
        return object == bitmapClass;
    }

    public boolean isBlockClosureClass(final ClassObject object) {
        return object == blockClosureClass;
    }

    public boolean isByteStringClass(final ClassObject object) {
        return object == byteStringClass;
    }

    public boolean isByteSymbolClass(final ClassObject object) {
        return object == getByteSymbolClass();
    }

    public boolean isFloatClass(final ClassObject object) {
        return object == floatClass;
    }

    public boolean isFullBlockClosureClass(final ClassObject object) {
        return object == fullBlockClosureClass;
    }

    public boolean isLargeIntegerClass(final ClassObject object) {
        return object == largePositiveIntegerClass || object == largeNegativeIntegerClass;
    }

    public boolean isMetaClass(final ClassObject object) {
        return object == metaClass;
    }

    public boolean isMethodContextClass(final ClassObject object) {
        return object == methodContextClass;
    }

    public boolean isNilClass(final ClassObject object) {
        return object == nilClass;
    }

    public boolean isPointClass(final ClassObject object) {
        return object == pointClass;
    }

    public boolean isSemaphoreClass(final ClassObject object) {
        return object == semaphoreClass;
    }

    public boolean isWideStringClass(final ClassObject object) {
        return object == getWideStringClass();
    }

    /*
     * INSTANCE CREATION
     */

    public ArrayObject asArrayOfLongs(final long... elements) {
        return ArrayObject.createWithStorage(this, arrayClass, elements);
    }

    public ArrayObject asArrayOfObjects(final Object... elements) {
        return ArrayObject.createWithStorage(this, arrayClass, elements);
    }

    public PointersObject asFraction(final long numerator, final long denominator, final AbstractPointersObjectWriteNode writeNode) {
        if (fractionClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final Object fractionLookup = lookup("Fraction");
            if (fractionLookup instanceof ClassObject) {
                fractionClass = (ClassObject) fractionLookup;
            } else {
                throw SqueakException.create("Unable to find Fraction class");
            }
        }
        final long actualNumerator;
        final long actualDenominator;
        if (denominator < 0) { // "keep sign in numerator"
            actualNumerator = -numerator;
            actualDenominator = -denominator;
        } else {
            actualNumerator = numerator;
            actualDenominator = denominator;
        }
        // Calculate gcd
        long n = actualNumerator;
        long m = actualDenominator;
        while (n != 0) {
            n = m % (m = n);
        }
        final long gcd = Math.abs(m);
        // Instantiate reduced fraction
        final PointersObject fraction = new PointersObject(this, fractionClass, fractionClass.getLayout());
        writeNode.execute(fraction, FRACTION.NUMERATOR, actualNumerator / gcd);
        writeNode.execute(fraction, FRACTION.DENOMINATOR, actualDenominator / gcd);
        return fraction;
    }

    public NativeObject asByteArray(final byte[] bytes) {
        return NativeObject.newNativeBytes(this, byteArrayClass, bytes);
    }

    public NativeObject asByteString(final String value) {
        return NativeObject.newNativeBytes(this, byteStringClass, MiscUtils.stringToBytes(value));
    }

    public NativeObject asByteSymbol(final String value) {
        CompilerAsserts.neverPartOfCompilation();
        return (NativeObject) asByteString(value).send(this, "asSymbol");
    }

    public NativeObject asWideString(final String value) {
        return NativeObject.newNativeInts(this, getWideStringClass(), MiscUtils.stringToCodePointsArray(value));
    }

    public NativeObject asString(final String value, final ConditionProfile wideStringProfile) {
        return wideStringProfile.profile(NativeObject.needsWideString(value)) ? asWideString(value) : asByteString(value);
    }

    public PointersObject asPoint(final AbstractPointersObjectWriteNode writeNode, final Object xPos, final Object yPos) {
        final PointersObject point = new PointersObject(this, pointClass);
        writeNode.execute(point, POINT.X, xPos);
        writeNode.execute(point, POINT.Y, yPos);
        return point;
    }

    public ArrayObject newEmptyArray() {
        return ArrayObject.createWithStorage(this, arrayClass, ArrayUtils.EMPTY_ARRAY);
    }

    public PointersObject newMessage(final AbstractPointersObjectWriteNode writeNode, final NativeObject selector, final ClassObject lookupClass, final Object[] arguments) {
        final PointersObject message = new PointersObject(this, messageClass);
        writeNode.execute(message, MESSAGE.SELECTOR, selector);
        writeNode.execute(message, MESSAGE.ARGUMENTS, asArrayOfObjects(arguments));
        assert message.instsize() > MESSAGE.LOOKUP_CLASS : "Early versions do not have lookupClass";
        writeNode.execute(message, MESSAGE.LOOKUP_CLASS, lookupClass);
        return message;
    }

    /*
     * INTEROP
     */

    @TruffleBoundary
    public NativeObject toInteropSelector(final Message message) {
        assert message.getLibraryClass() == InteropLibrary.class;
        return interopMessageToSelectorMap.computeIfAbsent(message, m -> {
            final String libraryName = message.getLibraryClass().getSimpleName();
            assert libraryName.endsWith("Library");
            final String libraryPrefix = libraryName.substring(0, 1).toLowerCase() + libraryName.substring(1, libraryName.length() - 7);
            final String messageName = message.getSimpleName();
            final String messageCapitalized = messageName.substring(0, 1).toUpperCase() + messageName.substring(1);
            final String suffix;
            switch (message.getParameterCount()) {
                case 1:
                    suffix = "";
                    break;
                case 2:
                    suffix = ":";
                    break;
                default:
                    final StringBuilder sb = new StringBuilder(":");
                    for (int i = 0; i < message.getParameterCount() - 2; i++) {
                        sb.append("and:");
                    }
                    suffix = sb.toString();
                    break;
            }
            return asByteSymbol(libraryPrefix + messageCapitalized + suffix);
        });
    }

    /*
     * PRINTING
     */

    @TruffleBoundary
    public void printToStdOut(final String string) {
        if (!options.isQuiet) {
            getOutput().println("[trufflesqueak] " + string);
        }
    }

    @TruffleBoundary
    public void printToStdOut(final Object... arguments) {
        printToStdOut(ArrayUtils.toJoinedString(" ", arguments));
    }

    @TruffleBoundary
    public void printToStdErr(final String string) {
        getError().println("[trufflesqueak] " + string);
    }

    @TruffleBoundary
    public void printToStdErr(final Exception e) {
        e.printStackTrace(getError());
    }

    @TruffleBoundary
    public void printToStdErr(final Object... arguments) {
        printToStdErr(ArrayUtils.toJoinedString(" ", arguments));
    }

    /*
     * INSTRUMENTATION
     */

    public <T> T reportAllocation(final T value) {
        if (allocationReporter.isActive()) {
            allocationReporter.onEnter(null, 0, AllocationReporter.SIZE_UNKNOWN);
            allocationReporter.onReturnValue(value, 0, AllocationReporter.SIZE_UNKNOWN);
        }
        return value;
    }
}
