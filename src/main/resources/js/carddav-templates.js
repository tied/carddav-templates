(function($){
    $(function(){
        $('tbody.com-mesilat-object-address td').each(function(){
            var $td = $(this);
            if ($td.text() === ''){
                $td.closest('tr').hide();
            }
        });
        $('tr.com-mesilat-object-phone').closest('table').width('100%');
        $('tr.com-mesilat-object-email').closest('table').width('100%');
        $('tbody.com-mesilat-object-address').closest('table').width('100%');
        $('tr.com-mesilat-object-url').closest('table').width('100%');
        
        $('td.com-mesilat-attribute-addresses').each(function(){
            var minThWidth = 10000;
            $(this).find('tbody.com-mesilat-object-address th:visible').each(function(){
                var $th = $(this);
                if ($th.width() < minThWidth){
                    minThWidth = $th.width();
                }
            });
            $(this).find('tbody.com-mesilat-object-address th').each(function(){
                $(this).width(minThWidth);
            });
        });
    });
})(AJS.$||$);

/*
 * To use country with icon

{
    getUrl: function(val) {
        if (val) {
            return AJS.contextPath() + '/rest/countries/1.0/find';
        } else {
            return null;
        }
    },
    getParams: function(autoCompleteControl, val){
        var params = {
            'max-results': 10
        };
        if (val) {
            params.filter = Confluence.unescapeEntities(val);
        }
        return params;
    },
    update: function(autoCompleteControl, link){
        //console.log('com.mesilat.carddav', link);

        function getHostAddress(){
            var url = window.location.href;
            var arr = url.split("/");
            return arr[0] + "//" + arr[2];
        };

        var code = link.restObj.id, // Country Code
        name = link.restObj.title; // Country Name
        var base64 = require('com.mesilat/base64');
        var def = base64.encode('{country-symbol:code=' + code + '}').replace('=','');
        var locale = $('meta[name="ajs-user-locale"]').attr('content');
        var href = AJS.contextPath() + '/plugins/servlet/confluence/placeholder/macro?definition=' + def + '&locale=' + locale + '&version=2';

        var ed = AJS.Rte.getEditor();
        var $span = $(ed.dom.create('span'), ed.getDoc());
        var $img = $('<img class="editor-inline-macro">');
        $img.attr('src', href);
        $img.attr('data-macro-name', 'country-symbol');
        $img.attr('data-macro-parameters', 'code=' + code);
        $img.attr('data-macro-schema-version', '1');
        $img.attr('data-mce-src', getHostAddress() + href);
        var $div = $('<div>');
        $img.appendTo($div);
        var $name = $('<span>');
        $name.text(name);
        $name.appendTo($div);

        $span.html($div.html());
        var tinymce = require('tinymce');
        tinymce.confluence.NodeUtils.replaceSelection($span);
    }
}
 */