package org.abc.perftest.model.member;

import java.util.ArrayList;
import java.util.Date;

public class Member {
    public String firstName;
    public String lastName;
    public String birthDate;
    public ArrayList<Phone> phones;
    public String email;
    public boolean hasPhoto;
    public String locationId;
    public EmploymentData employmentData;
    public boolean invalidAddress;
    public String number;
    public boolean usePrimaryMemberAddress;
    public String id;
    public String organizationId;
    public Address address;
    public PersonalDataHint personalDataHint;
    public boolean active;
    public String status;
    public String agreementNumber;
    public Date memberSince;
    public String memberSinceDate;
    public String primaryMemberNumber;
}