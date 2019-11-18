/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.store;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.rocketmq.common.UtilAll;
import static org.assertj.core.api.Assertions.assertThat;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

@Ignore
public class MappedFileQueueTest {
    @Test
    public void testGetLastMappedFile() {
        final String fixedMsg = "0123456789abcdef";

        MappedFileQueue mappedFileQueue =
            new MappedFileQueue("target/unit_test_store/a/", 1024, null);

        for (int i = 0; i < 1024; i++) {
            MappedFile mappedFile = mappedFileQueue.getLastMappedFile(0);
            assertThat(mappedFile).isNotNull();
            assertThat(mappedFile.appendMessage(fixedMsg.getBytes())).isTrue();
        }

        mappedFileQueue.shutdown(1000);
        mappedFileQueue.destroy();
    }

    @Test
    public void testFindMappedFileByOffset() {
        // four-byte string.
        final String fixedMsg = "abcd";

        MappedFileQueue mappedFileQueue =
            new MappedFileQueue("target/unit_test_store/b/", 1024, null);

        for (int i = 0; i < 1024; i++) {
            MappedFile mappedFile = mappedFileQueue.getLastMappedFile(0);
            assertThat(mappedFile).isNotNull();
            assertThat(mappedFile.appendMessage(fixedMsg.getBytes())).isTrue();
        }

        assertThat(mappedFileQueue.getMappedMemorySize()).isEqualTo(fixedMsg.getBytes().length * 1024);

        MappedFile mappedFile = mappedFileQueue.findMappedFileByOffset(0);
        assertThat(mappedFile).isNotNull();
        assertThat(mappedFile.getFileFromOffset()).isEqualTo(0);

        mappedFile = mappedFileQueue.findMappedFileByOffset(100);
        assertThat(mappedFile).isNotNull();
        assertThat(mappedFile.getFileFromOffset()).isEqualTo(0);

        mappedFile = mappedFileQueue.findMappedFileByOffset(1024);
        assertThat(mappedFile).isNotNull();
        assertThat(mappedFile.getFileFromOffset()).isEqualTo(1024);

        mappedFile = mappedFileQueue.findMappedFileByOffset(1024 + 100);
        assertThat(mappedFile).isNotNull();
        assertThat(mappedFile.getFileFromOffset()).isEqualTo(1024);

        mappedFile = mappedFileQueue.findMappedFileByOffset(1024 * 2);
        assertThat(mappedFile).isNotNull();
        assertThat(mappedFile.getFileFromOffset()).isEqualTo(1024 * 2);

        mappedFile = mappedFileQueue.findMappedFileByOffset(1024 * 2 + 100);
        assertThat(mappedFile).isNotNull();
        assertThat(mappedFile.getFileFromOffset()).isEqualTo(1024 * 2);

        // over mapped memory size.
        mappedFile = mappedFileQueue.findMappedFileByOffset(1024 * 4);
        assertThat(mappedFile).isNull();

        mappedFile = mappedFileQueue.findMappedFileByOffset(1024 * 4 + 100);
        assertThat(mappedFile).isNull();

        mappedFileQueue.shutdown(1000);
        mappedFileQueue.destroy();
    }

    @Test
    public void testFindMappedFileByOffset_StartOffsetIsNonZero() {
        MappedFileQueue mappedFileQueue =
            new MappedFileQueue("target/unit_test_store/b/", 1024, null);

        //Start from a non-zero offset
        MappedFile mappedFile = mappedFileQueue.getLastMappedFile(1024);
        assertThat(mappedFile).isNotNull();

        assertThat(mappedFileQueue.findMappedFileByOffset(1025)).isEqualTo(mappedFile);

        assertThat(mappedFileQueue.findMappedFileByOffset(0)).isNull();
        assertThat(mappedFileQueue.findMappedFileByOffset(123, false)).isNull();
        assertThat(mappedFileQueue.findMappedFileByOffset(123, true)).isEqualTo(mappedFile);

        assertThat(mappedFileQueue.findMappedFileByOffset(0, false)).isNull();
        assertThat(mappedFileQueue.findMappedFileByOffset(0, true)).isEqualTo(mappedFile);

        mappedFileQueue.shutdown(1000);
        mappedFileQueue.destroy();
    }

    @Test
    public void testAppendMessage() {
        final String fixedMsg = "0123456789abcdef";

        MappedFileQueue mappedFileQueue =
            new MappedFileQueue("target/unit_test_store/c/", 1024, null);

        for (int i = 0; i < 1024; i++) {
            MappedFile mappedFile = mappedFileQueue.getLastMappedFile(0);
            assertThat(mappedFile).isNotNull();
            assertThat(mappedFile.appendMessage(fixedMsg.getBytes())).isTrue();
        }

        assertThat(mappedFileQueue.flush(0)).isFalse();
        assertThat(mappedFileQueue.getFlushedWhere()).isEqualTo(1024);

        assertThat(mappedFileQueue.flush(0)).isFalse();
        assertThat(mappedFileQueue.getFlushedWhere()).isEqualTo(1024 * 2);

        assertThat(mappedFileQueue.flush(0)).isFalse();
        assertThat(mappedFileQueue.getFlushedWhere()).isEqualTo(1024 * 3);

        assertThat(mappedFileQueue.flush(0)).isFalse();
        assertThat(mappedFileQueue.getFlushedWhere()).isEqualTo(1024 * 4);

        assertThat(mappedFileQueue.flush(0)).isFalse();
        assertThat(mappedFileQueue.getFlushedWhere()).isEqualTo(1024 * 5);

        assertThat(mappedFileQueue.flush(0)).isFalse();
        assertThat(mappedFileQueue.getFlushedWhere()).isEqualTo(1024 * 6);

        mappedFileQueue.shutdown(1000);
        mappedFileQueue.destroy();
    }

    @Test
    public void testGetMappedMemorySize() {
        final String fixedMsg = "abcd";

        MappedFileQueue mappedFileQueue =
            new MappedFileQueue("target/unit_test_store/d/", 1024, null);

        for (int i = 0; i < 1024; i++) {
            MappedFile mappedFile = mappedFileQueue.getLastMappedFile(0);
            assertThat(mappedFile).isNotNull();
            assertThat(mappedFile.appendMessage(fixedMsg.getBytes())).isTrue();
        }

        assertThat(mappedFileQueue.getMappedMemorySize()).isEqualTo(fixedMsg.length() * 1024);
        mappedFileQueue.shutdown(1000);
        mappedFileQueue.destroy();
    }

    @Test
    public void testDeleteExpiredFileByOffset() {
        MappedFileQueue mappedFileQueue =
            new MappedFileQueue("target/unit_test_store/e", 5120, null);

        for (int i = 0; i < 2048; i++) {
            MappedFile mappedFile = mappedFileQueue.getLastMappedFile(0);
            assertThat(mappedFile).isNotNull();
            ByteBuffer byteBuffer = ByteBuffer.allocate(ConsumeQueue.CQ_STORE_UNIT_SIZE);
            byteBuffer.putLong(i);
            byte[] padding = new byte[12];
            Arrays.fill(padding, (byte) '0');
            byteBuffer.put(padding);
            byteBuffer.flip();

            assertThat(mappedFile.appendMessage(byteBuffer.array())).isTrue();
        }

        MappedFile first = mappedFileQueue.getFirstMappedFile();
        first.hold();

        assertThat(mappedFileQueue.deleteExpiredFileByOffset(20480, ConsumeQueue.CQ_STORE_UNIT_SIZE)).isEqualTo(0);
        first.release();

        assertThat(mappedFileQueue.deleteExpiredFileByOffset(20480, ConsumeQueue.CQ_STORE_UNIT_SIZE)).isGreaterThan(0);
        first = mappedFileQueue.getFirstMappedFile();
        assertThat(first.getFileFromOffset()).isGreaterThan(0);

        mappedFileQueue.shutdown(1000);
        mappedFileQueue.destroy();
    }

    @Test
    public void testDeleteExpiredFileByTime() throws Exception {
        MappedFileQueue mappedFileQueue =
            new MappedFileQueue("target/unit_test_store/f/", 1024, null);

        for (int i = 0; i < 100; i++) {
            MappedFile mappedFile = mappedFileQueue.getLastMappedFile(0);
            assertThat(mappedFile).isNotNull();
            byte[] bytes = new byte[512];
            assertThat(mappedFile.appendMessage(bytes)).isTrue();
        }

        assertThat(mappedFileQueue.getMappedFiles().size()).isEqualTo(50);
        long expiredTime = 100 * 1000;
        for (int i = 0; i < mappedFileQueue.getMappedFiles().size(); i++) {
            MappedFile mappedFile = mappedFileQueue.getMappedFiles().get(i);
            if (i < 5) {
                mappedFile.getFile().setLastModified(System.currentTimeMillis() - expiredTime * 2);
            }
            if (i > 20) {
                mappedFile.getFile().setLastModified(System.currentTimeMillis() - expiredTime * 2);
            }
        }
        mappedFileQueue.deleteExpiredFileByTime(expiredTime, 0, 0, false);
        assertThat(mappedFileQueue.getMappedFiles().size()).isEqualTo(45);
    }

    public static final Logger LOGGER = getLogger(MappedFileQueueTest.class);


    @Test
    public void testFindMappedFileByOffsetConcurrently() throws Exception {
        LOGGER.info("test");
        final byte[] data = new byte[256];
        final long testDataSize = data.length * 400000;
        final MappedFileQueue mappedFileQueue =
            new MappedFileQueue("target/unit_test_store/g", 1024, null);
        final AtomicLong offset = new AtomicLong(0);
        final AtomicBoolean running = new AtomicBoolean(true);
        Thread ct = new Thread(new Runnable() {
            @Override
            public void run() {
                while (offset.get() < testDataSize) {
                    if (mappedFileQueue.getMappedFiles().size() < 50) {
                        MappedFile mappedFile = mappedFileQueue.getLastMappedFile(offset.get());
                        assertThat(mappedFile).isNotNull();
                        assertThat(mappedFile.appendMessage(data)).isTrue();
                        offset.addAndGet(data.length);
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                running.set(false);
            }
        });
        ct.start();

        final AtomicLong readOffsetAL = new AtomicLong(0);
        Thread dt = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running.get()) {
                    if (mappedFileQueue.getMappedFiles().size() > 30) {
                        MappedFile mf = mappedFileQueue.getFirstMappedFile();
                        assertThat(mf).isNotNull();
                        if (mf.getFileFromOffset() + mappedFileQueue.getMappedFileSize() <= readOffsetAL.get()) {
                            assertThat(mf.destroy(120 * 1000)).isTrue();
                            mappedFileQueue.deleteExpiredFile(Lists.newArrayList(mf));
                        }
                    }
                }
            }
        });
        dt.start();



        while (running.get()) {
            long readOffset = readOffsetAL.get();
            if (readOffset < offset.get()) {
                MappedFile mf = mappedFileQueue.findMappedFileByOffset(readOffset);
                assertThat(mf).isNotNull();
                assertThat(mf.getFileFromOffset() <= readOffset && mf.getFileFromOffset() + mappedFileQueue.getMappedFileSize() > readOffset).isTrue();
                LOGGER.info("readOffset={}({}),mf={},offset={},size={}",
                    readOffset, offset.get() - readOffset, mf, offset.get(),
                    mappedFileQueue.getMappedFiles().size());

                readOffsetAL.getAndAdd(data.length);
            }
        }

        ct.join();
        dt.join();
        mappedFileQueue.shutdown(1000);
        mappedFileQueue.destroy();
    }

    @After
    public void destory() {
        File file = new File("target/unit_test_store");
        UtilAll.deleteFile(file);
    }
}
