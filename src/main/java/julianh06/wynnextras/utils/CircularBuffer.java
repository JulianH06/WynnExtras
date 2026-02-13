package julianh06.wynnextras.utils;

public class CircularBuffer<T> {
    private Object[] buffer;
    private int head = 0;  // Points to the oldest element
    private int tail = 0;  // Points to the next insertion point
    private int count = 0;
    private final int capacity;

    public CircularBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    public void insert(T item) {
        buffer[tail] = item;
        tail = (tail + 1) % capacity;

        if (count < capacity) {
            count++;
        } else {
            head = (head + 1) % capacity;  // Overwrite oldest
        }
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        // index 0 = most recent (last inserted)
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        // Calculate position: most recent is at (tail - 1) mod capacity
        int actualIndex = (tail - 1 - index + capacity) % capacity;
        return (T) buffer[actualIndex];
    }

    public int size() {
        return count;
    }

    public boolean isFull() {
        return count == capacity;
    }

    public void reset() {
        head = 0;
        tail = 0;
        count = 0;
        buffer = new Object[capacity];
    }
}
