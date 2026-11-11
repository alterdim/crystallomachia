package li.gerard.crystallomachia.golem;

public record GolemAnchor(double right, double up, double front, float yaw) {
    public static GolemAnchor inFront() {
        return new GolemAnchor(0, 0, 1, 180f);
    }

    public static GolemAnchor onLeft() {
        return new GolemAnchor(-1, 0, 0, -90f);
    }

    public static GolemAnchor onRight() {
        return new GolemAnchor(1, 0, 0, 90f);
    }
}
