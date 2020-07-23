/*
 * Copyright 2020 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.compression;

import com.github.luben.zstd.Zstd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static io.netty.handler.codec.compression.ZstdConstants.DEFAULT_BLOCK_SIZE;
import static io.netty.handler.codec.compression.ZstdConstants.HEADER_LENGTH;

public class ZstdTest {

    @Test
    public void testZstdCompressingDecompressingStream() throws IOException {

        String sample = randomString(DEFAULT_BLOCK_SIZE);
        ByteBuf source = Unpooled.directBuffer(DEFAULT_BLOCK_SIZE);
        source.writeBytes(sample.getBytes());

        ByteBuf target = Unpooled.directBuffer(DEFAULT_BLOCK_SIZE);
        int idx = target.writerIndex();
        ByteBuffer buffer = target.internalNioBuffer(idx + HEADER_LENGTH, target.writableBytes() - HEADER_LENGTH);

        int compressLength = Zstd.compress(
                buffer,
                source.internalNioBuffer(source.readerIndex(), source.readableBytes()),
                0);
        target.writerIndex(idx + HEADER_LENGTH + compressLength);

        ByteBuf output = Unpooled.directBuffer(DEFAULT_BLOCK_SIZE);
        ByteBuffer outputBuffer = output.internalNioBuffer(output.writerIndex(), output.writableBytes());
        ByteBuffer inputBuffer = target.internalNioBuffer(target.readerIndex() + HEADER_LENGTH, target.readableBytes() - HEADER_LENGTH);

        Zstd.decompress(outputBuffer, inputBuffer);

        outputBuffer.flip();
        output = Unpooled.wrappedBuffer(outputBuffer);
        String val = output.toString(Charset.defaultCharset());

        Assert.assertEquals(sample, val);
    }

    @Test
    public void testSimplestEncodeDecode() {
        String sample = randomString(1024);
        EmbeddedChannel channel = new EmbeddedChannel(new ZstdEncoder(), new ZstdDecoder());
        ByteBuf source = Unpooled.directBuffer();
        source.writeBytes(sample.getBytes());
        boolean writable = channel.writeOutbound(source);
        Assert.assertTrue(writable);
        ByteBuf buf = channel.readOutbound();
        Assert.assertNotNull(buf);
        Assert.assertTrue(channel.writeInbound(buf));

        buf = channel.readInbound();

        String val = buf.toString(Charset.defaultCharset());
        Assert.assertEquals(sample, val);
    }

    public static String randomString(int length) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + (int) (26 * Math.random())));
        }

        return sb.toString();
    }
}
