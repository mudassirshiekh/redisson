/**
 * Copyright (c) 2013-2024 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redisson.misc;

import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Nikita Koksharov
 *
 */
public final class SpinLock {

    private final AtomicReference<Thread> acquired = new AtomicReference<>();

    private final int spinLimit = 7000;
    private int nestedLevel;

    private void lockInterruptibly() throws InterruptedException {
        int spins = 0;
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            if (acquired.get() == Thread.currentThread()) {
                nestedLevel++;
                return;
            } else if (acquired.get() == null
                    && acquired.compareAndSet(null, Thread.currentThread())) {
                nestedLevel = 1;
                return;
            } else if (spins >= spinLimit) {
                Thread.yield();
            } else {
                spins++;
            }
        }
    }

    private void unlock() {
        if (acquired.get() == Thread.currentThread()) {
            nestedLevel--;
            if (nestedLevel == 0) {
                acquired.set(null);
            }
        }
    }

    public void execute(Runnable r) throws InterruptedException {
        lockInterruptibly();
        try {
            r.run();
        } finally {
            unlock();
        }
    }

}