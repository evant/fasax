package me.tatarka.fasax.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class Nodes implements Iterable<Node> {
    List<Node> nodes = new ArrayList<Node>();

    Nodes() {
        nodes = new ArrayList<Node>();
    }

    void add(Node node) {
        nodes.add(node);
    }

    boolean isEmpty() {
        return nodes.isEmpty();
    }

    int size() {
        return nodes.size();
    }

    Node get(int index) {
        return nodes.get(index);
    }

    Nodes attributes() {
        return filter(new Filter() {
            @Override
            public boolean accept(Node node) {
                return node.isAttribute();
            }
        });
    }

    Node text() {
        for (Node node : nodes) {
            if (node.isText()) return node;
        }
        return null;
    }

    Nodes onRoot() {
        return filter(new Filter() {
            @Override
            public boolean accept(Node node) {
                return !node.isAttribute() && !node.isText() && !node.isNested() && (!node.isList() || ((Node.IsList) node).isInline());
            }
        });
    }

    Nodes requiresState() {
        return filter(new Filter() {
            @Override
            public boolean accept(Node node) {
                return !node.isAttribute() && !node.isText() && (node.isNested() || (node.isList() && !(node.isNested() ||((Node.IsList) node).isInline())));
            }
        });
    }

    Nodes inlineList() {
        return filter(new Filter() {
            @Override
            public boolean accept(Node node) {
                return node.isList() && ((Node.IsList) node).isInline();
            }
        });
    }

    Nodes nested() {
        return filter(new Filter() {
            @Override
            public boolean accept(Node node) {
                return node.isNested();
            }
        });
    }

    Nodes filter(Filter filter) {
        Nodes result = new Nodes();
        for (Node node : nodes) {
            if (filter.accept(node)) {
                result.add(node);
            }
        }
        return result;
    }

    @Override
    public Iterator<Node> iterator() {
        return nodes.iterator();
    }

    interface Filter {
        boolean accept(Node node);
    }
}
