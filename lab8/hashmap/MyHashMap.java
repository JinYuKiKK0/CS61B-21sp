package hashmap;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 *  A hash table-backed Map implementation. Provides amortized constant time
 *  access to elements via get(), remove(), and put() in the best case.
 *
 *  Assumes null keys will never be inserted, and does not resize down upon remove().
 *  @author YOUR NAME HERE
 */
public class MyHashMap<K, V> implements Map61B<K, V>{

    /**
     * Protected helper class to store key/value pairs
     * The protected qualifier allows subclass access
     */
    protected class Node {
        K key;
        V value;

        Node(K k, V v) {
            key = k;
            value = v;
        }
    }

    /* Instance Variables */
    private Collection<Node>[] buckets;
    // You should probably define some more!
    private int size;
    private final double maxFactor;


    /** Constructors */
    public MyHashMap() {
        buckets = createTable(16);
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = createBucket();
        }
        size = 0;
        maxFactor = 0.75;
    }

    public MyHashMap(int initialSize) {
        buckets = createTable(initialSize);
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = createBucket();
        }
        size = 0;
        maxFactor = 0.75;
    }

    /**
     * MyHashMap constructor that creates a backing array of initialSize.
     * The load factor (# items / # buckets) should always be <= loadFactor
     *
     * @param initialSize initial size of backing array
     * @param maxLoad maximum load factor
     */
    public MyHashMap(int initialSize, double maxLoad) {
        buckets = createTable(initialSize);
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = createBucket();
        }
        size = 0;
        maxFactor = maxLoad;
    }

    /**
     * Returns a new node to be placed in a hash table bucket
     */
    private Node createNode(K key, V value) {
        return new Node(key,value);
    }

    /**
     * Returns a data structure to be a hash table bucket
     *
     * The only requirements of a hash table bucket are that we can:
     *  1. Insert items (`add` method)
     *  2. Remove items (`remove` method)
     *  3. Iterate through items (`iterator` method)
     *
     * Each of these methods is supported by java.util.Collection,
     * Most data structures in Java inherit from Collection, so we
     * can use almost any data structure as our buckets.
     *
     * Override this method to use different data structures as
     * the underlying bucket type
     *
     * BE SURE TO CALL THIS FACTORY METHOD INSTEAD OF CREATING YOUR
     * OWN BUCKET DATA STRUCTURES WITH THE NEW OPERATOR!
     */
    protected Collection<Node> createBucket() {
        return new LinkedList<>();
    }

    /**
     * Returns a table to back our hash table. As per the comment
     * above, this table can be an array of Collection objects
     *
     * BE SURE TO CALL THIS FACTORY METHOD WHEN CREATING A TABLE SO
     * THAT ALL BUCKET TYPES ARE OF JAVA.UTIL.COLLECTION
     *
     * @param tableSize the size of the table to create
     */
    private Collection<Node>[] createTable(int tableSize) {
        return new Collection[tableSize];
    }

    // TODO: Implement the methods of the Map61B Interface below
    // Your code won't compile until you do so!

    /*
    return the index corresponding to the key's hashCode;
     */
    private int hash(K key){
        return Math.floorMod(key.hashCode(), buckets.length);
    }
    private int hash(K key,int length){
        return Math.floorMod(key.hashCode(),length);
    }

    private void resize(){
        /*
        create a new array with length*factor to buckets
        reload all of items with new hashCode to the new buckets
         */
        Collection<Node>[] tempBuckets = createTable(buckets.length * 2);
        for (int i = 0; i < tempBuckets.length; i++) {
            tempBuckets[i] = createBucket();
        }

        for (int i = 0; i < buckets.length; i++) {
            if(buckets[i] !=null){
                for (Node node : buckets[i]) {
                    Node newNode = createNode(node.key,node.value);
                    int newIndex = hash(node.key, tempBuckets.length);
                    tempBuckets[newIndex].add(newNode);
                }
            }
        }
        buckets = tempBuckets;
    }

    /**
     * Through key's hashcode find bucket and find node in bucket
     * @param key
     * @return if node exists return The node corresponding to the key
     * else return false
     */
    private Node findNodeByKeyHash(K key){
        int index = hash(key);
        if(buckets[index] == null){
            buckets[index] = createBucket();
        }
        for (Node node : buckets[index]) {
            if(node.key.equals(key)){
                return node;
            }
        }
        return null;
    }
    /**
     * Removes all of the mappings from this map.
     */
    @Override
    public void clear() {
        buckets = createTable(buckets.length);
        size = 0;
    }

    /**
     * Returns true if this map contains a mapping for the specified key.
     *
     * @param key
     */
    @Override
    public boolean containsKey(K key) {
        /*hash(key)->index corresponding to a bucket to place it in
        * get the bucket by indexing into the buckets array
        * if buckets contains the key given ,return true;
        * otherwise return false
        * */
        return findNodeByKeyHash(key) != null;
    }

    /**
     * Returns the value to which the specified key is mapped, or null if this
     * map contains no mapping for the key.
     *
     * @param key
     */
    @Override
    public V get(K key) {
        if(containsKey(key)){
            Node node = findNodeByKeyHash(key);
            return node.value;
        }
        return null;
    }

    /**
     * Returns the number of key-value mappings in this map.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key,
     * the old value is replaced.
     *
     * @param key
     * @param value
     */
    @Override
    public void put(K key, V value) {
        /*contain key  if key exists update its value
        otherwise add key-value pair into corresponding bucket through hashcode

        keep track of the size and when to resize
        * */
        Node node = findNodeByKeyHash(key);
        int index = hash(key);
        if (node != null) {
            node.value = value;
        } else {
            if(buckets[index] == null){
                buckets[index] = createBucket();
            }
            buckets[index].add(createNode(key, value));
            size++;
        }

        double loadFactor = (double) size / buckets.length;
        if(loadFactor >= maxFactor){
            resize();
        }
    }

    /**
     * Returns a Set view of the keys contained in this map.
     */
    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     * Not required for Lab 8. If you don't implement this, throw an
     * UnsupportedOperationException.
     *
     * @param key
     */
    @Override
    public V remove(K key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the entry for the specified key only if it is currently mapped to
     * the specified value. Not required for Lab 8. If you don't implement this,
     * throw an UnsupportedOperationException.
     *
     * @param key
     * @param value
     */
    @Override
    public V remove(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<K> iterator() {
        throw new UnsupportedOperationException();
    }
}
