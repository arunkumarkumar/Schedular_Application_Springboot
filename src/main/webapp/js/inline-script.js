
//---------mobile menu height---------//
$(document).ready(
    function () {
        function setHeight() {
            windowHeight = $(window).innerHeight();
            $('.report-table-container').height(
                $(window).height() - 87 - $("header").height() - $(".head-content").height());
        };
        setHeight();

        $(window).resize(function () {
            setHeight();
        });

        $(window).scroll(function () {
            setHeight();
        });

        //         $(".showhide-filter-btn, #txtSearch").click(function () {
        //                 $(window).trigger('resize');                				
        //});

    });
//---------mobile menu height---------//


//---------mobile menu btn---------//
$(document).ready(function () {
    $(".menu-btn").click(function () {
        $(".admin-body-container").toggleClass("hide-submenu");
    });
});
//---------mobile menu btn---------//

//---------Page Loader---------//
$(document).ready(function () {
    $(document).ajaxStart(function () {
        $("#loader").css("display", "table");
    });
    $(document).ajaxComplete(function () {
        $("#loader").css("display", "none");
        $(window).trigger('resize');
    });

});
//---------Page Loader---------//

//PopUp
$(document).ready(function () {
    $('#usermanagement .add-user-btn').click(function () {
        $('.popup-body-container#addeditUserPop').removeClass("hide");
        $('#addeditUserPophead').text('Add User');
    });
    $('#usermanagement .edit-btn').click(function () {
        $('.popup-body-container#addeditUserPop').removeClass("hide");
        $('#addeditUserPophead').text('Edit User');

    });

    $('#rolemanagement .add-role-btn').click(function () {
        $('.popup-body-container#addeditRolePop').removeClass("hide");
        $('#addeditRolePophead').text('Add Role');
    });
    $('#rolemanagement .edit-btn').click(function () {
        $('.popup-body-container#addeditRolePop').removeClass("hide");
        $('#addeditRolePophead').text('Edit Role');

    });


    $('#dbconfigui .add-db-ui-btn').click(function () {
        $('.popup-body-container#addeditdbuiPop').removeClass("hide");
        $('#addeditdbuiPophead').text('Add Database Configuration');
    });
    $('#dbconfigui .edit-btn').click(function () {
        $(this).closest("tr").addClass("editable-field");
        $('.popup-body-container#addeditdbuiPop').removeClass("hide");
        $('#addeditdbuiPophead').text('Edit Database Configuration');

    });



    $('#dbconfigui .accept-btn').click(function () {
        $('.popup-body-container#checkerdbuiPop').removeClass("hide");

    });

    $('#checkerdbuiPop #dbuiAccept').click(function () {
        $('.popup-body-container#checkerdbuiPop').addClass("hide");
        $('.popup-body-container#addMessagePop').removeClass("hide");
        $('.popup-body-container#addMessagePop .popup-header').removeClass('cancel-del-pop-header');
        $('#messagedbuiPophead').text('Confirm Database Configuration ');
        $('.msgpopuptxt').text('Do you want to confirm this table format?');        
    });

    $('#checkerdbuiPop #dbuiReject').click(function () {
        $('.popup-body-container#checkerdbuiPop').addClass("hide");
        $('.popup-body-container#addMessagePop').removeClass("hide");
        $('.popup-body-container#addMessagePop .popup-header').addClass('cancel-del-pop-header');
        $('#messagedbuiPophead').text('Reject Database Configuration ');
        $('.msgpopuptxt').text('Do you want to reject this table format?');
    });
    


       
    $('.popup-close-btn').click(function () {
        $('.popup-body-container').addClass("hide");
    });

});
//PopUp



//audio player checked
$(document).on("click", '.audio-radiobtn-switch label', function () {
    $('.upload-audio-list > tr').removeClass('enable-audio');
    $(this).closest('tr').addClass('enable-audio');
});
//audio player checked