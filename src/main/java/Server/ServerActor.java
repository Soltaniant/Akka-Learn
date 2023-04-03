package Server;

import akka.actor.*;
import Messages.Message.*;
import java.util.HashMap;

public class ServerActor extends AbstractActor
{

    HashMap<String, ActorRef> usersInformation = new HashMap<>();
    HashMap<String, ActorRef> groupsInformation = new HashMap<>();

    @Override
    public Receive createReceive()
    {
        return receiveBuilder()
                .match(ConnectRequestMessage.class, this::OnConnect)
                .match(DisconnectMessage.class, this::OnDisconnect)
                .match(PrivateUserTextMessage.class, this::OnPrivateMessage)
                .match(PrivateFileMessage.class, this::OnPrivateFile)
                .match(CreateGroupRequestMessage.class, this::OnCreateGroup)
                .match(LeaveGroupRequestMessage.class, this::OnLeaveGroup)
                .match(GroupMessage.class, this::OnGroupMessage)
                .match(GroupFileMessage.class, this::OnGroupFile)
                .match(InviteToGroupMessage.class, this::OnInviteToGroup)
                .match(RemoveFromGroupMessage.class, this::OnRemoveFromGroup)
                .match(CloseGroupMessage.class, this::OnCloseGroup)
                .match(AddCoadminGroupMessage.class, this::OnAddCoadminGroup)
                .match(RemoveCoadminGroupMessage.class, this::OnRemoveCoadminGroup)
                .match(MuteGroupMessage.class, this::OnMuteGroup)
                .match(UnMuteGroupMessage.class, this::OnUnmuteGroup).build();
    }

    private void OnUnmuteGroup(UnMuteGroupMessage message) {
        if(doesGroupExist(message.groupName, sender()) && doesUserExist(message.target, sender()))
        {
            ActorRef groupActor = groupsInformation.get(message.groupName);
            message.userActor = sender();
            message.targetActor = usersInformation.get(message.target);
            groupActor.forward(message, getContext());
        }
    }

    private void OnMuteGroup(MuteGroupMessage message) {
        if(doesGroupExist(message.groupName, sender()) && doesUserExist(message.target, sender()))
        {
            ActorRef groupActor = groupsInformation.get(message.groupName);
            message.userActor = sender();
            message.targetActor = usersInformation.get(message.target);
            groupActor.forward(message, getContext());
        }
    }

    private void OnRemoveCoadminGroup(RemoveCoadminGroupMessage message) {
        if(doesGroupExist(message.groupName, sender()) && doesUserExist(message.userToRemove, sender()))
        {
            ActorRef groupActor = groupsInformation.get(message.groupName);
            message.userToRemoveActor = usersInformation.get(message.userToRemove);
            groupActor.forward(message, getContext());
        }
    }

    private void OnAddCoadminGroup(AddCoadminGroupMessage message) {
        if(doesGroupExist(message.groupName, sender()) && doesUserExist(message.userToAdd, sender()))
        {
            ActorRef groupActor = groupsInformation.get(message.groupName);
            message.userToAddActor = usersInformation.get(message.userToAdd);
            groupActor.forward(message, getContext());
        }
    }

    private void OnCloseGroup(CloseGroupMessage message) {
        if(doesGroupExist(message.groupName, sender()))
        {
            groupsInformation.get(message.groupName).tell(akka.actor.PoisonPill.getInstance(), self());
            groupsInformation.remove(message.groupName);
        }
    }

    private void OnRemoveFromGroup(RemoveFromGroupMessage message) {
        if(doesGroupExist(message.groupName, sender()) && doesUserExist(message.targetName, sender()))
        {
            ActorRef groupActor = groupsInformation.get(message.groupName);
            message.userActor = sender();
            message.targetActor = usersInformation.get(message.targetName);
            groupActor.forward(message, getContext());
        }
    }

    private void OnInviteToGroup(InviteToGroupMessage message) {
        if(doesGroupExist(message.groupName, sender()) && doesUserExist(message.targetName, sender()))
        {
            ActorRef groupActor = groupsInformation.get(message.groupName);
            message.userActor = sender();
            message.targetActor = usersInformation.get(message.targetName);
            groupActor.forward(message, getContext());
        }
    }

    private void OnGroupFile(GroupFileMessage message) {
        if(doesGroupExist(message.groupName, sender()))
        {
            ActorRef groupActor = groupsInformation.get(message.groupName);
            groupActor.forward(message, getContext());
        }
    }

    private void OnGroupMessage(GroupMessage message) {
        if(doesGroupExist(message.groupName, sender()))
        {
            ActorRef groupActor = groupsInformation.get(message.groupName);
            groupActor.forward(message, getContext());
        }
    }

    private void OnLeaveGroup(LeaveGroupRequestMessage message) {
        if(doesGroupExist(message.groupName, sender()))
        {
            ActorRef groupActor = groupsInformation.get(message.groupName);
            groupActor.forward(message, getContext());
        }
    }

    private void OnCreateGroup(CreateGroupRequestMessage message) {
        if(groupsInformation.containsKey(message.groupName))
        {
            OtherMessage msg = new OtherMessage();
            msg.text = message.groupName + " already exists!";
            sender().tell(msg, ActorRef.noSender());
        }
        else
        {//create new groupActor, add it to groups list and send relevant message to the user created it.
            ActorRef groupActor = getContext().actorOf(Props.create(GroupActor.class, message.groupName), "Group" + message.groupName);
            groupsInformation.put(message.groupName, groupActor);

            message.userActor = usersInformation.get(message.userName);
            groupActor.forward(message, getContext());

            CreateGroupApproveMessage msg = new CreateGroupApproveMessage();
            msg.groupActor = groupActor;
            msg.userName = message.userName;
            msg.groupName = message.groupName;
            sender().tell(msg, ActorRef.noSender());
        }
    }

    private void OnPrivateFile(PrivateFileMessage message) {
        if(doesUserExist(message.target, sender()))
        {
            ActorRef target = usersInformation.get(message.target);
            target.forward(message, getContext());
        }
    }

    private void OnPrivateMessage(PrivateUserTextMessage message) {
        if(doesUserExist(message.target, sender()))
        {
            ActorRef target = usersInformation.get(message.target);
            target.forward(message, getContext());
        }
    }

    private void OnDisconnect(DisconnectMessage message) {
        ActorRef clientActor;
        if(!usersInformation.containsKey(message.userName))
        {//User is not in connected users list - user is already disconnected/ doesn't exist
            clientActor = sender();
            OtherMessage msg = new OtherMessage();
            msg.text = message.userName + " is already disconnected/ doesn't exist";
            clientActor.tell(msg, ActorRef.noSender());
        }
        else
        {//Remove user from connected users list and sent disconnect message to the user
            usersInformation.remove(message.userName);
            clientActor = sender();
            clientActor.tell(message, ActorRef.noSender());
//                        clientActor.tell(akka.actor.PoisonPill.getInstance(), self());
        }
    }

    private void OnConnect(ConnectRequestMessage message) {
        if(!usersInformation.containsKey(message.userName))
        {//username is not in use
            ActorRef clientActor = sender();
            ActorSelection serverActor = getContext().actorSelection("akka.tcp://System@127.0.0.1:8000/user/Server");
            usersInformation.put(message.userName, clientActor);

            //send connection message to the client
            ConnectRequestMessage connectMessage = new ConnectRequestMessage();
            connectMessage.server = serverActor;
            clientActor.tell(connectMessage, self());

            //send connection request to the server
//                        connectMessage.client = clientActor;
//                        serverActor.tell(connectMessage, self());
        }
        else
        {//Server holds user name in its users map, user name is in use
            OtherMessage msg = new OtherMessage();
            msg.text = message.userName +" is in use!";
            sender().tell(msg, ActorRef.noSender());
        }
    }

    private boolean doesUserExist(String userName, ActorRef userActor)
    {//TODO: make it 1 func with boolean identifier
        if(usersInformation.containsKey(userName))
            return true;
        SendNotExistMessage(userName, userActor);
        return false;
    }

    private boolean doesGroupExist(String groupName, ActorRef userActor)
    {
        if(groupsInformation.containsKey(groupName))
            return true;
        SendNotExistMessage(groupName, userActor);
        return false;
    }

    private void SendNotExistMessage(String missing, ActorRef targetActor)
    {//Send info not exist message to the given user actor
        OtherMessage msg  = new OtherMessage();
        msg.text = missing + " does not exist!";
        targetActor.tell(msg, ActorRef.noSender());
    }
}
