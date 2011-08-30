#!/bin/bash
echo -n novell user name: 
read -e novell_user
sudo ncpmount tcp -S 131.170.189.124 -A 131.170.189.124 -U $novell_user.staff.gssp.dsc.rmit /media/novell -u root -g rdv -f 660

