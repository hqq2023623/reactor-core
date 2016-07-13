/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.core.queue;

import java.util.function.Supplier;

import reactor.core.util.Exceptions;
import reactor.core.util.Sequence;

abstract class NotFunRingBufferFields<E> extends RingBuffer<E>
{
    private final   long               indexMask;
    private final   Object[]           entries;
    protected final int                bufferSize;
    protected final RingBufferProducer sequenceProducer;

    NotFunRingBufferFields(Supplier<E> eventFactory,
                     RingBufferProducer sequenceProducer)
    {
        this.sequenceProducer = sequenceProducer;
        this.bufferSize = sequenceProducer.getBufferSize();

        if (bufferSize < 1)
        {
            throw new IllegalArgumentException("bufferSize must not be less than 1");
        }

        this.indexMask = bufferSize - 1;
        this.entries   = new Object[sequenceProducer.getBufferSize()];
        fill(eventFactory);
    }

    private void fill(Supplier<E> eventFactory)
    {
        for (int i = 0; i < bufferSize; i++)
        {
            entries[i] = eventFactory.get();
        }
    }

    @SuppressWarnings("unchecked")
    protected final E elementAt(long sequence)
    {
        return (E) entries[(int) (sequence & indexMask)];
    }
}

/**
 * Ring based store of reusable entries containing the data representing
 * an event being exchanged between event producer and ringbuffer consumers.
 *
 * @param <E> implementation storing the data for sharing during exchange or parallel coordination of an event.
 */
final class NotFunRingBuffer<E> extends NotFunRingBufferFields<E>
{
    /**
     * Construct a RingBuffer with the full option set.
     *
     * @param eventFactory to newInstance entries for filling the RingBuffer
     * @param sequenceProducer sequencer to handle the ordering of events moving through the RingBuffer.
     * @throws IllegalArgumentException if bufferSize is less than 1 or not a power of 2
     */
    NotFunRingBuffer(Supplier<E> eventFactory,
                     RingBufferProducer sequenceProducer)
    {
        super(eventFactory, sequenceProducer);
    }

    @Override
    public E get(long sequence)
    {
        return elementAt(sequence);
    }

    @Override
    public long next()
    {
        return sequenceProducer.next();
    }

    @Override
    public long next(int n)
    {
        return sequenceProducer.next(n);
    }

    @Override
    public long tryNext() throws Exceptions.InsufficientCapacityException
    {
        return sequenceProducer.tryNext();
    }

    @Override
    public long tryNext(int n) throws Exceptions.InsufficientCapacityException
    {
        return sequenceProducer.tryNext(n);
    }

    @Override
    public void resetTo(long sequence)
    {
        sequenceProducer.claim(sequence);
        sequenceProducer.publish(sequence);
    }

    @Override
    public void addGatingSequence(Sequence gatingSequence)
    {
        sequenceProducer.addGatingSequence(gatingSequence);
    }

    @Override
    public long getMinimumGatingSequence()
    {
        return getMinimumGatingSequence(null);
    }

    @Override
    public long getMinimumGatingSequence(Sequence sequence)
    {
        return sequenceProducer.getMinimumSequence(sequence);
    }

    @Override
    public boolean removeGatingSequence(Sequence sequence)
    {
        return sequenceProducer.removeGatingSequence(sequence);
    }

    @Override
    public RingBufferReceiver newBarrier()
    {
        return sequenceProducer.newBarrier();
    }

    @Override
    public long getCursor()
    {
        return sequenceProducer.getCursor();
    }

    @Override
    public Sequence getSequence()
    {
        return sequenceProducer.getSequence();
    }

    @Override
    public int bufferSize()
    {
        return bufferSize;
    }

    @Override
    public void publish(long sequence)
    {
        sequenceProducer.publish(sequence);
    }

    @Override
    public void publish(long lo, long hi)
    {
        sequenceProducer.publish(lo, hi);
    }

    @Override
    public long remainingCapacity()
    {
        return sequenceProducer.remainingCapacity();
    }

    @Override
    public RingBufferProducer getSequencer() {
        return sequenceProducer;
    }


}
