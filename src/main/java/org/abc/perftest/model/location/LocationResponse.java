package org.abc.perftest.model.location;

import java.util.ArrayList;

public class LocationResponse {
    public ArrayList<Location> content = new ArrayList <Location> ();
    public Pageable pageable;
    public boolean last;
    public long totalElements;
    public long totalPages;
    public boolean first;
    public Sort sort;
    public long number;
    public long numberOfElements;
    public long size;
    public boolean empty;
}