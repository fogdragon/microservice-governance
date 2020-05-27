package com.storm.monitor.core.entity;

public class DiskVolumeInfo {

    private String m_id;

    private long m_total;

    private long m_free;

    private long m_usable;

    public DiskVolumeInfo() {
    }

    public DiskVolumeInfo(String id) {
        m_id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DiskVolumeInfo) {
            DiskVolumeInfo _o = (DiskVolumeInfo) obj;
            String id = _o.getId();

            return m_id == id || m_id != null && m_id.equals(id);
        }

        return false;
    }

    public long getFree() {
        return m_free;
    }

    public String getId() {
        return m_id;
    }

    public long getTotal() {
        return m_total;
    }

    public long getUsable() {
        return m_usable;
    }

    @Override
    public int hashCode() {
        int hash = 0;

        hash = hash * 31 + (m_id == null ? 0 : m_id.hashCode());

        return hash;
    }

    public void mergeAttributes(DiskVolumeInfo other) {

        m_total = other.getTotal();

        m_free = other.getFree();

        m_usable = other.getUsable();
    }

    public DiskVolumeInfo setFree(long free) {
        m_free = free;
        return this;
    }

    public DiskVolumeInfo setId(String id) {
        m_id = id;
        return this;
    }

    public DiskVolumeInfo setTotal(long total) {
        m_total = total;
        return this;
    }

    public DiskVolumeInfo setUsable(long usable) {
        m_usable = usable;
        return this;
    }

}
