/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */
import React, { Component } from 'react';
import {
  AppRegistry,
  StyleSheet,
  Text,
  View,
  TextInput,
  NativeModules,
  processColor,
  TouchableOpacity,
  Image,
  AsyncStorage,
  StatusBar,
  Button,
  Modal
} from 'react-native';

import KSYVideo from './KSYVideo';
import AudioController from './AudioController'
import { GiftedChat } from 'react-native-gifted-chat';
import Moment from 'moment';

import webstomp from './stomp/webstomp.js';
import {SOCKET_BASE_URL, SOCKET_PUBLISH, SOCKET_SUBCRIBE, Command, Action} from "../api/Config";
import {subscribeRoom} from "../api/WebsocketHelper"

export default class RoomScreen extends Component {
    constructor(props) {
      super(props);
      var data = this.props.navigation.state.params.data;
      var userId = data.roomUserState.externalUserId;
      this.state = {
        showbar: true,
        degree: 0 , mirror: false, volume: 0.5,
        messages: [],
        userId: userId,
        ws:null,
        hasLiked: false,
        wsClient:null,
        likeCounts: 0,
        viewCounts: 0,
        data:data,
        token:this.props.navigation.state.params.token,
        modalVisibility:true,
        roomId : data.roomUserState.roomId,
        };

      //bind methodss
      this.onReceivedMessage = this.onReceivedMessage.bind(this);
      this.onSend = this.onSend.bind(this);
      this._storeMessages = this._storeMessages.bind(this);
      this.startWebSocket = this.startWebSocket.bind(this);
      this.onUpdateRoomStats = this.onUpdateRoomStats.bind(this);
      this.onUpdateChatMessage = this.onUpdateChatMessage.bind(this);
      this.onUpdateRoomStats = this.onUpdateRoomStats.bind(this);
      this.updateLikeButton = this.updateLikeButton.bind(this);
      this.sendLike = this.sendLike.bind(this);
      this.onCloseScreen = this.onCloseScreen.bind(this);
      this.sendViaWs = this.sendViaWs.bind(this);
      this.getRoomStats = this.getRoomStats.bind(this);

      this.startWebSocket();

      console.log("--------->play mediaUrl: " + data.mediaUrl);
    }

    onPressRotate(){
        //this.state.degree=(this.state.degree+90)%360;
        // this.video.setRotateDegree(this.state.degree);
        this.setState({degree: (this.state.degree+90)%360});
    }

    onAudioProgressChanged(newPercent) {
        if (newPercent >= 0){
          this.setState({volume: newPercent/100});
        }
    }

    componentWillUnmount(){
      console.log("------------> Close Screen");
      this.onCloseScreen();
    }
    render() {
    const {params} = this.props.navigation.state;
    const {volume} = this.state;
    var user = { _id: this.state.userId || -1 };
        return (

      <View style={styles.container}>
      <View style={styles.video}>

          <StatusBar
              hidden={!this.state.showbar}
            />
            <KSYVideo
            ref={(video)=>{this.video = video}}
            source={{uri:this.state.data.mediaUrl}}
            bufferSize={30}
            bufferTime={4}
            repeat={true}
            mirror={this.state.mirror}
            degree={this.state.degree}
            resizeMode={'contain'}
            volume={volume}
            onLoad={(data)=>{console.log("JS onPrepared, video size = " + data.naturalSize.width + "x" +  data.naturalSize.height);}}
            onEnd={(data)=>{console.log("JS onCompletion");}}
            onError={(data)=>{console.log("JS onError:" + data.error.what + data.error.extra);}}
            style={styles.fullScreen}
          />

          {this.state.showbar?(
            <View style={{justifyContent:'flex-end' , flexDirection: 'row', width:260, marginBottom:6}}>
              <Text style={styles.text}>View: {this.state.viewCounts} </Text>
              <Text style={styles.text}>Like: {this.state.likeCounts} </Text>
              <View>
                <Button
                  containerStyle={{margin: 5, flex: 1}}
                  buttonStyle={{height: 48}}
                  title={this.state.hasLiked ? "Unlike" : "Like"}
                  style={styles.button}
                  onPress={()=>this.sendLike()}>
                </Button>
              </View>
            </View>):(null)}

            </View>

            <GiftedChat
              style={styles.comments}
              messages={this.state.messages}
              onSend={this.onSend}
              user={user}
            />

      </View>

    );
  }


//===========COMMENTS==========================================

  startWebSocket(){

    const options = { debug: true, protocols: webstomp.VERSIONS.supportedProtocols() };

    var wsUri = SOCKET_BASE_URL;

    var client = webstomp.over(new WebSocket(wsUri), options);

    var headers = {
      'token': this.state.token,
      'type' : 'ROOM',
    };
    client.connect(headers, (frame)=>{
        console.log("-----> CONNECTED");
        //set object state
        this.setState({wsClient:client});
        //subscribe chanel
        subscribeRoom(headers, client, this.state.roomId, (roomId, messages)=>{this.onReceivedMessage(roomId, messages);});

        // client.subscribe(SOCKET_SUBCRIBE+this.state.roomId,this.onReceivedMessage);
        //send command to get room stats
        this.getRoomStats();
    },
    (error)=>{
      console.log("--------> on ERROR " + error);
    }
  );
  }

    // Event listeners
    /**
     * When the server sends a message to this.
     */
    onReceivedMessage(roomId, messages) {
      console.log("--------> message: "+ messages.body);
      // console.log("--------> onReceivedMessage: " + messages +": "+ messages);
      // this._storeMessages(messages);
      var body = messages.body;

      //TODO fake data for testing because there is an issue in this case
      // if(body.includes(Command.ROOM_STATS))
      //   body = '{"command":null,"target":null,"action":"ROOM_STATS","data":{"userCount": 10, "likeCount": 10}}';

      var msg = JSON.parse(body);
      var command = msg.command;
      var data = msg.data;
      var action = msg.action;
      this.onHandleCommand(command,action, data);
    }

    /**
      When received message, we will get command and handle it
      There are 2 commands: Chat and update room stats
    */
    onHandleCommand(command,action, data){
      if(command == Command.COMMENT){
        this.onUpdateChatMessage(data);
      }else if(command == Command.LIKE){
        this.updateLikeButton(data);
      }else if(action == Command.ROOM_STATS){
        this.onUpdateRoomStats(data);
      }
    }

    /**
      convert data from server to GiftedChat model and show it on UI

      Server data structure:
       {long id,String externalUserId,String content,
       long time, boolean isActive, String quoteUser,
       String quoteContent,String quoteTime}

      GiftedChat data structure: { _id: 1,
        text: 'My message',
        createdAt: new Date(Date.UTC(2016, 5, 11, 17, 20, 0)),
        user: {
          _id: 2,
          name: 'React Native',
          avatar: 'https://facebook.github.io/react/img/logo_og.png',
        },
        image: 'https://facebook.github.io/react/img/logo_og.png',
        // Any additional custom parameters are passed through
      }
    */
    onUpdateChatMessage(data){
      var userId = this.state.userId;

      if(data.externalUserId)
        userId = data.externalUserId;

      console.log("----------> User Id: " + userId +"/"+ data.content);
      //convert to form of GiftedChat
      var message =[{ _id:data.id,
        text: data.content,
        createdAt: new Date(),
        user: {
          _id: userId,
          name: userId,
        },
      }];
      console.log("----------> Message: " + message[0]);
      //_storeMessages
      this._storeMessages(message);
    }
    /**
    When update room stats, we need to show the data on UI
    */
    onUpdateRoomStats(data){
      this.setState({
        likeCounts: data.likeCount,
        viewCounts: data.userCount,
      })
    }
    /**
     * When a message is sent, send the message to the server
     * and store it in this component's state.
     data structure: {String content,String quoteUser, String quoteContent, String quoteTime}
     {token: token, command: commandValue, target: targetValue, action: actionValue, data: dataValue}
     */
    onSend(messages=[]) {
      console.log("onSend " + messages[0].text);
      if(this.state.ws){
        this.state.ws.send(JSON.stringify(messages[0]));
      }
        //convert GiftedChat data structure to server structure
        var msg ={
          token: this.state.token,
          command : Command.COMMENT,
          action : Action.CHAT_PUBLISH,
          target: null,
          data: {
            'content':messages[0].text,
            'quoteUser': null,
            'quoteContent': null,
            'quoteTime': null,
          }
        };

        console.log("-----------> send message: " +  JSON.stringify(msg));
        this.sendViaWs(JSON.stringify(msg));
    }

    //send command to get room stats

    getRoomStats(){
      var like = !this.state.hasLiked;

      var data ={
        token: this.state.token,
        command : Command.ROOM_STATS,
        data: null
      };

      this.sendViaWs(JSON.stringify(data));
    }
    //send command like
    sendLike(){
      var like = !this.state.hasLiked;

      var msg ={
        token: this.state.token,
        command : Command.LIKE,
        data: like
      };

      this.sendViaWs(JSON.stringify(msg));
      this.updateLikeButton(like);
    }

    updateLikeButton(like){
      this.setState({hasLiked: like});
    }
    // Helper functions
    _storeMessages(messages) {
      console.log("----------> store message: " + JSON.stringify(messages));
      this.setState((previousState) => {
        return {
          messages: GiftedChat.append(previousState.messages, messages),
        };
      });
    }

    onCloseScreen(){
      //close socket
      if(this.state.wsClient){
          this.state.wsClient.disconnect(()=>{console.log("------------> DISCONNECTED");}, {});
      }
      //close ui
      this.setState({modalVisibility:false});
    }

    sendViaWs(data){
      console.log("-----------> send data: "+ data);
      if(this.state.wsClient){
        this.state.wsClient.send(SOCKET_PUBLISH+this.state.roomId,data);
      }
    }
}
  // ==============STYLE==================

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'column',
    justifyContent: 'space-between',
    backgroundColor: 'white',
  },
  text: {
    fontSize: 14,
    color: 'white',
  },
  button: {
    flex: 1,
    height:48,
    fontSize: 14,
    color: 'white',
  },
  fullScreen: {
    position: 'absolute',
    top: 0,
    left: 0,
    bottom: 0,
    right: 0,
    backgroundColor: 'black',
  },

 videoView: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center"
  },
  comments: {
     flex: 1,
     justifyContent: "center",
     alignItems: "center",
     backgroundColor: 'white'
   },
  video: {
     flexDirection: 'row',
     height:200,
     justifyContent: 'space-between',
     backgroundColor: 'black',
   },
  controller: {
    height: 30,
    width: 250,
    justifyContent: "center",
    alignItems: "center"
  },
  progressBar: {
    alignSelf: "stretch",
    margin: 30
  },
});
