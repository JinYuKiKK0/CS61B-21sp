package bstmap;

import java.util.Iterator;
import java.util.Set;

public class BSTMap<K extends Comparable<K>, V> implements Map61B {
    int size = 0;
    /*
    keys and values are stored in a linked list of Entry objects.
    This variable stores the first pair in this linked list.
     */
    private BSTNode BST;

    private class BSTNode {
        private K key;
        private V value;
        private BSTNode left;
        private BSTNode right;

        BSTNode(K key, V value) {
            this.key = key;
            this.value = value;
        }

        /**
         * 根据给定的key值，找出与key对应的BSTNode
         * 找到了返回Node，没找到返回null
         */
        private BSTNode getNode(K k) {
            int cmp = k.compareTo(key);
            if (cmp == 0) {
                return this;
            }
            if (cmp < 0) {//说明待查找的k<key，进入左子树
                return (left == null) ? null : left.getNode(k);
            } else {
                return (right == null) ? null : right.getNode(k);
            }
        }

        private BSTNode put(K k, V v) {
            int cmp = k.compareTo(key);
            if (cmp == 0) {
                value = v;
                return this;
            } else if (cmp < 0) {
                if (left == null) {
                    left = new BSTNode(k, v);
                    size++;
                    return left;
                }
                return left.put(k, v);
            } else {
                if (right == null) {
                    right = new BSTNode(k, v);
                    size++;
                    return right;
                }
                return right.put(k, v);
            }
        }
    }

    /**
     * Removes all of the mappings from this map.
     */
    @Override
    public void clear() {
        size = 0;
        BST = null;
    }

    /* Returns true if this map contains a mapping for the specified key. */
    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            return false;
        }
        K k = (K) key;
        if (BST == null) {
            return false;
        }
        return BST.getNode(k) != null;
    }

    /* Returns the value to which the specified key is mapped, or null if this
     * map contains no mapping for the key.
     */
    @Override
    public Object get(Object key) {
        if (key == null) {
            return null;
        }
        K k = (K) key;
        BSTNode node = (BST == null) ? null : BST.getNode(k);
        return (node == null) ? null : node.value;
    }

    /* Returns the number of key-value mappings in this map. */
    @Override
    public int size() {
        return size;
    }

    /* Associates the specified value with the specified key in this map. */
    @Override
    public void put(Object key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        K k = (K) key;
        V v = (V) value;
        if (BST == null) {
            BST = new BSTNode(k, v);
            size = 1;
        } else {
            BST.put(k, v);
        }
    }

    //print BSTMap by up key order
    public void printInOrder() {
    }

    @Override
    public Set keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator iterator() {
        throw new UnsupportedOperationException();
    }

}