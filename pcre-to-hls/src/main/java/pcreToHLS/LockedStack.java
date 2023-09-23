package pcreToHLS;

import java.util.Stack;

public class LockedStack<T> {
    private Stack<T> stack;
    private boolean locked;

    public LockedStack()
    {
        this.stack = new Stack<>();
        this.locked = false;
    }

    public void lock()
    {
        this.locked = true;
    }

    public boolean isLocked()
    {
        return this.locked;
    }

    public T peek()
    {
        return this.stack.peek();
    }

    public T pop(boolean force)
    {
        T ret = null;
        if (!this.locked || force)
            ret = this.stack.pop();
        return ret;
    }

    public T pop()
    {
        return this.pop(false);
    }

    public void push(T new_element)
    {
        this.push(new_element, false);
    }

    public void push(T new_element, boolean force)
    {
        if (!this.locked || force)
            this.stack.push(new_element);
    }
}
