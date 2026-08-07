package julianh06.wynnextras.compat.wynntils;

import julianh06.wynnextras.functions.RaidFunctions;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public final class WynntilsFunctionAdapter {
    private static boolean attempted;

    private WynntilsFunctionAdapter() {}

    public static synchronized void registerRaidFunction(Object manager) {
        if (attempted || manager == null) return;
        attempted = true;
        try {
            Class<?> functionClass = WynntilsCompat.requireClass("com.wynntils.core.consumers.functions.Function");
            Class<?> adapter = defineAdapter(functionClass);
            Object function = adapter.getConstructor().newInstance();
            Method register = findMethod(manager.getClass(), "registerFunction", 1);
            if (!register.trySetAccessible()) return;
            register.invoke(manager, function);
        } catch (Throwable error) {
            julianh06.wynnextras.core.WynnExtras.LOGGER.warn(
                    "Wynntils raid function integration is unavailable; continuing without it", error);
        }
    }

    public static Object raidValue(Object arguments) {
        try {
            return RaidFunctions.getRaidDrop(argument(arguments, "raid"), argument(arguments, "mode"), argument(arguments, "type"));
        } catch (Throwable ignored) {
            return -1L;
        }
    }

    public static Object createArgumentsBuilder() {
        try {
            Class<?> argumentClass = WynntilsCompat.requireClass(
                    "com.wynntils.core.consumers.functions.arguments.Argument");
            Constructor<?> constructor = null;
            for (Constructor<?> candidate : argumentClass.getConstructors()) {
                if (candidate.getParameterCount() == 3
                        && candidate.getParameterTypes()[0] == String.class
                        && candidate.getParameterTypes()[1] == Class.class) {
                    constructor = candidate;
                    break;
                }
            }
            if (constructor == null) return null;
            List<Object> arguments = List.of(constructor.newInstance("raid", String.class, null),
                    constructor.newInstance("mode", String.class, null),
                    constructor.newInstance("type", String.class, null));
            Class<?> builder = WynntilsCompat.requireClass(
                    "com.wynntils.core.consumers.functions.arguments.FunctionArguments$RequiredArgumentBuilder");
            return builder.getConstructor(List.class).newInstance(arguments);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String translation(String suffix) {
        return RaidFunctions.getTranslation(suffix);
    }

    private static String argument(Object arguments, String name) throws ReflectiveOperationException {
        Object argument = arguments.getClass().getMethod("getArgument", String.class).invoke(arguments, name);
        return String.valueOf(argument.getClass().getMethod("getStringValue").invoke(argument));
    }

    private static Class<?> defineAdapter(Class<?> functionClass) throws IllegalAccessException {
        String className = "julianh06/wynnextras/compat/wynntils/GeneratedRaidDropFunction";
        String superName = Type.getInternalName(functionClass);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, className, null, superName, null);
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();

        for (Method method : functionClass.getMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) continue;
            emitMethod(writer, method);
        }
        writer.visitEnd();
        return MethodHandles.lookup().defineClass(writer.toByteArray());
    }

    private static void emitMethod(ClassWriter writer, Method method) {
        String descriptor = Type.getMethodDescriptor(method);
        MethodVisitor visitor = writer.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), descriptor, null, null);
        visitor.visitCode();
        String owner = Type.getInternalName(WynntilsFunctionAdapter.class);
        switch (method.getName()) {
            case "getName" -> visitor.visitLdcInsn("wynnextras_raid_drop");
            case "getTypeName" -> visitor.visitLdcInsn("WynnExtrasFunction");
            case "getValue" -> {
                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "raidValue", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            }
            case "getArgumentsBuilder" -> visitor.visitMethodInsn(Opcodes.INVOKESTATIC, owner,
                    "createArgumentsBuilder", "()Ljava/lang/Object;", false);
            case "getTranslation" -> {
                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "translation",
                        "(Ljava/lang/String;)Ljava/lang/String;", false);
            }
            default -> {
                emitDefault(visitor, Type.getReturnType(method));
                visitor.visitMaxs(0, 0);
                visitor.visitEnd();
                return;
            }
        }
        emitObjectReturn(visitor, Type.getReturnType(method));
        visitor.visitMaxs(0, 0);
        visitor.visitEnd();
    }

    private static void emitObjectReturn(MethodVisitor visitor, Type returnType) {
        if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
            visitor.visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
            visitor.visitInsn(Opcodes.ARETURN);
        } else {
            emitDefault(visitor, returnType);
        }
    }

    private static void emitDefault(MethodVisitor visitor, Type type) {
        switch (type.getSort()) {
            case Type.VOID -> visitor.visitInsn(Opcodes.RETURN);
            case Type.LONG -> { visitor.visitInsn(Opcodes.LCONST_0); visitor.visitInsn(Opcodes.LRETURN); }
            case Type.FLOAT -> { visitor.visitInsn(Opcodes.FCONST_0); visitor.visitInsn(Opcodes.FRETURN); }
            case Type.DOUBLE -> { visitor.visitInsn(Opcodes.DCONST_0); visitor.visitInsn(Opcodes.DRETURN); }
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> {
                visitor.visitInsn(Opcodes.ICONST_0); visitor.visitInsn(Opcodes.IRETURN);
            }
            default -> { visitor.visitInsn(Opcodes.ACONST_NULL); visitor.visitInsn(Opcodes.ARETURN); }
        }
    }

    private static Method findMethod(Class<?> type, String name, int parameters) throws NoSuchMethodException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameters) return method;
            }
        }
        throw new NoSuchMethodException(name);
    }
}
