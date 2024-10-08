package com.splitwise.microservices.user_service.controller;

import com.splitwise.microservices.user_service.entity.Group;
import com.splitwise.microservices.user_service.entity.GroupMemberDetails;
import com.splitwise.microservices.user_service.entity.User;
import com.splitwise.microservices.user_service.mapper.GroupMapper;
import com.splitwise.microservices.user_service.mapper.UserMapper;
import com.splitwise.microservices.user_service.model.GroupDataResponse;
import com.splitwise.microservices.user_service.model.UserModel;
import com.splitwise.microservices.user_service.service.GroupService;
import com.splitwise.microservices.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/group")
public class GroupController {

    @Autowired
    GroupService groupService;
    @Autowired
    UserService userService;
    @Autowired
    UserMapper userMapper;
    @Autowired
    GroupMapper groupMapper;

    @PostMapping("/create-group")
    public ResponseEntity<String> createGroup(@RequestBody Group group)
    {
        if(group == null)
        {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        //Save group details
        Group savedGroup = groupService.saveGroupDetails(group);
        if(savedGroup == null)
        {
            return new ResponseEntity<>("Error occurred while saving Group Details",HttpStatus.INTERNAL_SERVER_ERROR);
        }
        //Add the creator as a Group Member
        GroupMemberDetails groupMemberDetails = groupMapper.getGroupMemberDetailsFromGroup(savedGroup);
        groupService.addGroupMember(groupMemberDetails);
        return new ResponseEntity<>("Group Details Saved Successfully",HttpStatus.OK);
    }

    @PostMapping("/add-members")
    public ResponseEntity<String> addGroupMembers(@RequestBody GroupMemberDetails groupMemberDetails)
    {
        if(groupMemberDetails == null)
        {
            return new ResponseEntity<>("Invalid input to add members",HttpStatus.NOT_FOUND);
        }
        GroupMemberDetails groupMemberDetails1 = groupService.addGroupMember(groupMemberDetails);
        return new ResponseEntity<>("Member added successfully to the group",HttpStatus.OK);

    }
    @PutMapping("/update-group")
    public ResponseEntity<String> updateGroup(@RequestBody Group group)
    {
        if(group == null || group.getGroupId()==null)
        {
            return new ResponseEntity<>("Error occurred while updating Group",HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Group updatedGroup = groupService.saveGroupDetails(group);
        return new ResponseEntity<>("Group updated successfully",HttpStatus.OK);
    }
    @GetMapping("/get-members/{groupId}")
    public ResponseEntity<List<UserModel>> getGroupMembers(@PathVariable("groupId") Long groupId)
    {
        if(groupId == null)
        {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        //Get Member Ids
        List<Long> memberIds = groupService.getAllUserIdByGroupId(groupId);
        if(memberIds == null || memberIds.isEmpty())
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        //Get User Details with Member Ids
        List<User> users = userService.getUsersDetailById(memberIds);
        if(users == null || users.isEmpty())
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        List<UserModel> memberDetailsList = userMapper.getUserModel(users);
        return new ResponseEntity<>(memberDetailsList,HttpStatus.OK); //Need to convert to json and return
    }

    /**
     * This method return list groups a user participating
     * @param userId
     */
    @GetMapping("/get-all-groups/{userId}")
    public ResponseEntity<List<GroupDataResponse>> getAllTheGroupOfUser(@PathVariable("userId") Long userId)
    {
        List<Long> groupIds = groupService.getAllGroupIdsOfUser(userId);
        if(groupIds == null || groupIds.isEmpty())
        {
            return new ResponseEntity<>(new ArrayList<>(),HttpStatus.NOT_FOUND);
        }
        List<GroupDataResponse> groupDataResponse = groupService.getUserGroupDataList(groupIds);
        return new ResponseEntity<>(groupDataResponse, HttpStatus.OK);
    }

    @GetMapping("get-all-group-map/{userId}")
    public ResponseEntity<Map<Long,String>> getAllGroupNameMapByUserId(@PathVariable("userId") Long userId)
    {
        if(userId ==null)
        {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Map<Long,String> groupNameMap = groupService.getGroupNameMapByUserId(userId);
        if(groupNameMap == null)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(groupNameMap,HttpStatus.OK);
    }

    @GetMapping("/get-group/{groupId}")
    public ResponseEntity<GroupDataResponse> getGroupDataByUserId(@PathVariable("groupId") Long groupId)
    {
        if(groupId == null)
        {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        GroupDataResponse groupDataResponse = groupService.getUserDataByGroupId(groupId);
        return new ResponseEntity<>(groupDataResponse,HttpStatus.OK);
    }

    @GetMapping("/get-group-name/{groupId}")
    public ResponseEntity<String> getGroupNameById(@PathVariable("groupId") Long groupId)
    {
        if(groupId == null)
        {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        String groupName = groupService.getGroupNameById(groupId);
        if(!StringUtils.hasLength(groupName))
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(groupName,HttpStatus.OK);
    }

    @GetMapping("/get-group-name-map/{groupId}")
    public ResponseEntity<Map<Long,String>> getGroupNameMapByGroupId(@PathVariable("groupId") Long groupId){
        if(groupId == null)
        {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Map<Long, String> groupNameMap = new HashMap<>();
        String groupName = groupService.getGroupNameById(groupId);
        if(groupName != null && groupName.length()>0)
        {
            groupNameMap.put(groupId, groupName);
        }
        if(!StringUtils.hasLength(groupName))
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(groupNameMap,HttpStatus.OK);
    }

    /**
     * Method to delete the group (Only Admin can perform this)
     * @param groupId
     */
    @DeleteMapping("/delete-group/{groupId}")
    public void deleteGroup(@PathVariable("groupId") Long groupId)
    {
        //Todo add check for Admin user of the group
        groupService.deleteGroupAndMembers(groupId);
    }

    @DeleteMapping("/{userId}/leave-group/{groupId}")
    public ResponseEntity<String> leaveGroup(@PathVariable("userId") Long userId,@PathVariable Long groupId)
    {
        //Todo Do check for any pending out standings
        boolean isDeleted = groupService.deleteGroupMember(userId,groupId);
        if(isDeleted)
        {
            return new ResponseEntity<>("You Left the group successfully", HttpStatus.OK);
        }
        return new ResponseEntity<>("Error occurred while performing leave group",HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
