package org.bitcoin.core;

import com.google.common.util.concurrent.CycleDetectingLockFactory;

import java.util.concurrent.locks.ReentrantLock;

public class Threading {
    public static CycleDetectingLockFactory factory;


    public static ReentrantLock lock(Class<ConnectionHandler> clazz) {
        return lock(clazz.getSimpleName() + " lock");
    }

    public static ReentrantLock lock(String name) {
        if (Utils.isAndroidRuntime()) //如果是安卓
            return new ReentrantLock(true);
        else
            return factory.newReentrantLock(name); //如果不是安卓，则通过工厂创建
    }


}
