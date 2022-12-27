package org.abc.perftest.model.member;

import java.util.ArrayList;

public class MemberResponse {
    public ArrayList<Member> content;
    public Pageable pageable;
    public boolean last;
    public int totalElements;
    public int totalPages;
    public Sort sort;
    public boolean first;
    public int number;
    public int numberOfElements;
    public int size;
    public boolean empty;
}