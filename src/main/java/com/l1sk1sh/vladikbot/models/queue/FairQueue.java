package com.l1sk1sh.vladikbot.models.queue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @param <T>
 * @author John Grosh (jagrosh)
 */
public class FairQueue<T extends Queueable> {
    private final List<T> list = new ArrayList<>();
    private final Set<Long> set = new HashSet<>();

    public final int add(T item) {
        int lastIndex;
        for (lastIndex = list.size() - 1; lastIndex > -1; lastIndex--) {
            if (list.get(lastIndex).getIdentifier() == item.getIdentifier()) {
                break;
            }
        }
        lastIndex++;
        set.clear();
        for (; lastIndex < list.size(); lastIndex++) {
            if (set.contains(list.get(lastIndex).getIdentifier())) {
                break;
            }
            set.add(list.get(lastIndex).getIdentifier());
        }
        list.add(lastIndex, item);
        return lastIndex;
    }

    public final void addAt(int index, T item) {
        if (index >= list.size()) {
            list.add(item);
        } else {
            list.add(index, item);
        }
    }

    public final int size() {
        return list.size();
    }

    public final T pull() {
        return list.remove(0);
    }

    public final boolean isEmpty() {
        return list.isEmpty();
    }

    public final List<T> getList() {
        return list;
    }

    public final T get(int index) {
        return list.get(index);
    }

    public final void remove(int index) {
        list.remove(index);
    }

    public final int removeAll(long identifier) {
        int count = 0;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).getIdentifier() == identifier) {
                list.remove(i);
                count++;
            }
        }
        return count;
    }

    public final void clear() {
        list.clear();
    }

    public final int shuffle(long identifier) {
        List<Integer> iset = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getIdentifier() == identifier) {
                iset.add(i);
            }
        }
        for (int j = 0; j < iset.size(); j++) {
            int first = iset.get(j);
            int second = iset.get((int) (Math.random() * iset.size()));
            T temp = list.get(first);
            list.set(first, list.get(second));
            list.set(second, temp);
        }
        return iset.size();
    }

    public final void skip(int number) {
        if (number > 0) {
            list.subList(0, number).clear();
        }
    }

    public final T moveItem(int from, int to) {
        T item = list.remove(from);
        list.add(to, item);
        return item;
    }
}
