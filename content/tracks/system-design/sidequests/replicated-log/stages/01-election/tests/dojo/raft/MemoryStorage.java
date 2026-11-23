package dojo.raft;

/** A disk in memory: it survives the node, the way a disk survives a crashed process. */
final class MemoryStorage implements Storage {
    private Persistent state = Persistent.EMPTY;
    int saves;

    @Override
    public Persistent load() {
        return state;
    }

    @Override
    public void save(Persistent state) {
        saves++;
        this.state = state;
    }
}
