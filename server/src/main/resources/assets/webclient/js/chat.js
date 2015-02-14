$(function(){
    var accessTokenExists = function() {
        return typeof $.cookie('token') !== 'undefined';
    };

    var redirectToSignin = function() {
        window.location.replace("/signin.html")
    };

    if(!accessTokenExists()) {
        redirectToSignin();
    }

    var $messageContainer = $('#chat-message-container');
    $messageContainer.on('new-message', function(e, message, me) {
    console.log(me);
        $(this).append($("<li/>").html(message.body).addClass(me ? "me" : ""));
    });

    $messageInputForm = $('#message-form');
    $messageInputForm.find("input").on("keyup change", function() {
        $messageInputForm.find("button").prop('disabled', $(this).val() == "");
    }).trigger("change");

    var connect = function() {
        var socket = new WebSocket("ws://"+window.location.hostname+":8080/chat");

        socket.addEventListener("open", function(event) {
            $('.loading-container').hide();
            $('#chat-container').removeClass('hidden');
        });

        socket.addEventListener("message", function(event) {
            $messageContainer.trigger('new-message', [JSON.parse(event.data), false]);
        });

        socket.addEventListener("error", function(event) {
            alert("An error occurred...");
            console.log(event);
        });

        socket.addEventListener("close", function(event) {
            if(event.code == 403) {
                redirectToSignin();
            }
            else {
                alert("Disconnected...");
            }
            console.log(event);
        });

        $messageInputForm.submit(function(e){
            e.preventDefault();
            if(socket.readyState === 1) {
                var input = $messageInputForm.find('input').val();
                if(input != "") {
                    var msg = {
                        "@type":"SendMessage",
                        "body":$messageInputForm.find('input').val(),
                        "uuid":""
                    };
                    socket.send(JSON.stringify(msg));
                    $messageInputForm.find('input').val("");
                    $messageContainer.trigger('new-message', [msg, true]);
                }
            }
        });
    };

    connect();
});