#!/bin/bash -e
#
#
# Add users into Apache realm (for ELK and Solr)
#
#

elkAdminUser=elkadmin
solrAdminUser=solradmin
apacheAdminUser=apacheadmin
pathPasswordFile=/opt/datafari/apache/password

 addUser() {
        user=${1}
        password=${2}
        realm=datafari
        digest="$( printf "%s:%s:%s" "$user" "$realm" "$password" | md5sum | awk '{print $1}' )"
        printf "%s:%s:%s\n" "$user" "$realm" "$digest" >> "${pathPasswordFile}/htpasswd"
}

echo "Script for add users to Apache proxy config"
echo "Do you wish to continue ? y/n"
read continue_script
if [[ $continue_script = n ]] ; then
	exit 1;
fi

echo "Do you want to add ELK admin user ? y/n"
read elk_user_choice

if [[ $elk_user_choice = y ]] ; then
echo "Enter the password that you want for the user elkadmin"
read elk_user_password
addUser $elkAdminUser $elk_user_password 
echo "the new account for ELK is created"
fi

echo "Do you want to add a Solr admin user (y/n) ?"
read solr_user_choice

if [[ $solr_user_choice = y ]] ; then
echo "Enter the password that you want for the user solradmin"
read solr_user_password
addUser $solrAdminUser $solr_user_password 
echo "the new account for Solr is created"
fi

echo "Do you want to change the password for the global admin user named apacheadmin ? (y/n)"
read admin_user_choice
if [[ $admin_user_choice = y ]] ; then
echo "Enter the password that you want for the user admin"
read admin_user_password
addUser $apacheAdminUser $admin_user_password 
echo "The password for admin user is set. Edit now the file ${pathPasswordFile}/htpasswd and delete the first entry of the file that starts with admin:datafari "
fi
echo "Please restart Apache : service apache2 restart on Debian or apachectl restart on CentOS/RedHat"



