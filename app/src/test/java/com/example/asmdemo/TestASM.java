package com.example.asmdemo;

import org.junit.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class TestASM {

    @Test
    public void test() throws Exception {
        // 要操作的class源文件，这里换成你本机的路径
        String originClzPath = "/Users/xxx/IdeaProjects/ASMDemo/app/build/intermediates/javac/debug/classes/com/example/asmdemo/MainActivity.class";
        FileInputStream fis = new FileInputStream(originClzPath);
        // ClassReader是ASM提供的读取字节码的工具
        ClassReader classReader = new ClassReader(fis);
        // ClassWriter是ASM提供的写入字节码的工具
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
        // 自定义类访问器，在其中完成对某个方法的字节码操作
        MyClassVisitor myClassVisitor = new MyClassVisitor(Opcodes.ASM5, classWriter);
        // 调用ClassReader的accept方法开始处理字节码
        classReader.accept(myClassVisitor, ClassReader.EXPAND_FRAMES);
        // 操作后的class文件写入到这个文件中，为了方便对比，这里创建了MainActivity2.class
        String destPath = "/Users/xxx/IdeaProjects/ASMDemo/app/build/intermediates/javac/debug/classes/com/example/asmdemo/MainActivity2.class";
        // 通过ClassWriter拿到处理后的字节码对应的字节数组
        byte[] bytes = classWriter.toByteArray();
        FileOutputStream fos = new FileOutputStream(destPath);
        // 写文件
        fos.write(bytes);
        // 关闭文件流
        fos.close();
        fis.close();
    }

    class MyClassVisitor extends ClassVisitor {

        public MyClassVisitor(int api, ClassVisitor cv) {
            super(api, cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            // 访问类时会调用该方法
            // visit: name = com/example/asmdemo/MainActivity
            System.out.println("visit: name = " + name);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            // 访问类中的方法时会调用visitMethod
            // visitMethod: name = <init> // 代表构造方法
            // visitMethod: name = onCreate
            System.out.println("visitMethod: name = " + name);
            if ("onCreate".equals(name)) {
                MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
                return new MyAdviceAdapter(Opcodes.ASM5, methodVisitor, access, name, desc);
            }
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    class MyAdviceAdapter extends AdviceAdapter {

        private int startTimeId;

        protected MyAdviceAdapter(int api, MethodVisitor mv, int access, String name, String desc) {
            super(api, mv, access, name, desc);
        }

        /**
         * 这个方法的目的是为了在onCreate方法开始处插入如下代码：
         * long v = System.currentTimeMillis();
         */
        @Override
        protected void onMethodEnter() {
            super.onMethodEnter();
            // 在方法开始处调用
            // 创建一个long类型的本地变量
            startTimeId = newLocal(Type.LONG_TYPE);
            // 调用System.currentTimeMillis()方法
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            // 将上一步中的结果保存到startTimeId指向的long类型变量中（不是保存到startTimeId）
            mv.visitIntInsn(LSTORE, startTimeId);
        }

        /**
         * 这个方法的目的是在onCreate方法结束的地方插入如下代码：
         * long end = System.currentTimeMillis();
         * long delta = end - start;
         * System.out.println("execute onCreate() use time: " + delta);
         */
        @Override
        protected void onMethodExit(int opcode) {
            super.onMethodExit(opcode);
            // 在方法结束时调用
            // 创建一个long类型的本地变量
            int endTimeId = newLocal(Type.LONG_TYPE);
            // 调用System.currentTimeMillis()方法
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            // 将上一步中的结果保存到endTimeId指向的long类型变量中（不是保存到endTimeId）
            mv.visitIntInsn(LSTORE, endTimeId);
            // 创建一个long类型的本地变量，deltaTimeId为这个变量的ID
            int deltaTimeId = newLocal(Type.LONG_TYPE);
            // 加载endTimeId指向的long类型的变量
            mv.visitIntInsn(LLOAD, endTimeId);
            // 加载startTimeId指向的long类型变量
            mv.visitIntInsn(LLOAD, startTimeId);
            // 将上面两个变量做减法（endTimeIdVal - startTimeIdVal）
            mv.visitInsn(LSUB);
            // 将减法的结果存在deltaTimeId指向的变量中
            mv.visitIntInsn(LSTORE, deltaTimeId);
            // 调用System静态方法out
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            // 创建StringBuilder对象
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            // 复制栈顶数值并将复制值压入栈顶
            mv.visitInsn(DUP);
            // 调用StringBuilder构造方法初始化
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            // 将字符串推到栈顶
            mv.visitLdcInsn("execute onCreate() use time: ");
            // 调用StringBuilder的append方法
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            // 加载deltaTimeId指向的long类型数据
            mv.visitVarInsn(LLOAD, deltaTimeId);
            // 调用StringBuilder的append方法
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false);
            // 调用StringBuilder的toString方法
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            // 调用System.out的println方法
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }
    }

}
