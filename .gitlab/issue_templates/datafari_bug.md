>>>
Template to declare a bug. 

First, please use the label "bug" to flag it as such.

You have 4 priorities to choose from: Prio 1 is for critical matters, Prio 2 is for enhancements meant to be pushed within the next 2 releases, Prio 3 is for things that matter but you're not sure when to push it, Prio 4 is for enhancements that have clearly no priority at all at the time of its creation.

You should create one ticket per Datafari version where it will be pushed, and use the linking mechanism to point to the same tickets between different versions. Versions are declared as Milestones in our Gitlab.

You cannot declare Datafari versions impacted by the bug as a metadata, so you have to declare it in your description.

Once you have closed the ticket, don't forget to mention as comment the commit.

When you close the ticket, you can use labels to declare you have NOT solved it: duplicate, wontfix, invalid and cannot reproduce. Please don't forget to use them.
>>>

## DESCRIPTION
#### What is the bug about. Be as accurate as possible (OS, Datafari version, scenario...)
#### This is mandatory


## VERSION CONCERNED
#### On which version of Datafari was the bug present 
#### This is not the target version for the fix, as this is a milestone.

## CHECKLIST BEFORE CLOSING TICKET
- [ ] Documentation
  - [ ] I have created the functional documentation in the wiki
  - [ ] I have created the technical documentation in the wiki 
  - [ ] I have added javadoc comments on key functions in my code
- [ ] Security 
  - [ ] I have cleaned up any input coming from users
  - [ ] I have not put any token APIs, passwords or the like in my code
  - [ ] I am not using 3rd party libraries that are deprecated or not maintained
