class OrderDTO {

    int id
    String name
    String level;

    String toString() {
        return "Order " + id + " from " + name + " (" + level + ")"
    }
}
