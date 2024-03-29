#Copyright (C) 2022  Riley Kuttruff
#
#	This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or
#	(at your option) any later version.
#
#	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
#	Public License for more details.
#
#	You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
#

# Based on https://gist.github.com/clarketm/dc5d5be390e3f811a2dd7f5e8c5728ba

# pyinstaller -F auth.py

import os, sys, platform

from os.path import exists

import json, requests

from oauth2client.client import OAuth2WebServerFlow
from oauth2client.tools import run_flow
from oauth2client.file import Storage

from dotenv import load_dotenv

from datetime import datetime, timedelta
from dateutil import parser

load_dotenv()

username = os.getenv('username')

def disable_stout():
	o_stdout = sys.stdout
	o_file = open(os.devnull, "w")
	sys.stdout = o_file
	return (o_stdout, o_file)

def enable_stout(o_stdout, o_file):
	o_file.close()
	sys.stdout = o_stdout
	
os_type = platform.system()

STORE = None

if os_type == 'Windows':
	STORE = os.getenv("APPDATA") + '\\smtp-client\\creds.data'
elif os_type == 'Linux' or os_type == 'Darwin':
	STORE = os.path.expanduser('~/.smtpc/creds.data')
else:
	STORE = 'creds.data'

if os.getenv('uselocalstate') is not None:
	STORE = 'creds.data'

# STORE = 'creds.data'
NEWSTORE = STORE + '.new'

def tryRefresh(clientId, clientSecret, refreshToken):
	url = 'https://oauth2.googleapis.com/token'
	
	auth = (clientId, clientSecret)
	
	params = {
		"grant_type":"refresh_token",
		"refresh_token":refreshToken
	}
	
	ret = requests.post(url, auth=auth, data=params)
	
	if ret.status_code != 200:
		return (False, None)
	else:
		data = ret.json()
		return (True, data)

def convertIfNeeded(data):
	if 'users' not in data.keys():
		newData = {}
		# newData['users'] = [{
			# "username": username,
			# "data": data
		# }]
		
		newData['users'] = []
		return newData
	else:
		return data

def getToken():
	CLIENT_ID = os.getenv('CLIENT_ID')
	CLIENT_SECRET = os.getenv('CLIENT_SECRET')
#	SCOPE = 'https://www.googleapis.com/auth/gmail.send'
	SCOPE = 'https://mail.google.com/'
	REDIRECT_URL = 'http://127.0.0.1'
	
	if exists(STORE):
		f = open(STORE, "r")
		
		creddata = json.load(f)
		
		data = None
		
		for d in creddata['users']:
			if d['username'] == username:
				data = d['data']
				break
			
		now = datetime.now().isoformat()
		
		if data is not None:
			if now < data['token_expiry']:
				print(data['access_token'])
				sys.exit(0)
			else:
				success, resp = tryRefresh(CLIENT_ID, CLIENT_SECRET, data['refresh_token'])
				
				if success:
					at = resp['access_token']
					exp = resp['expires_in']
					
					data['access_token'] = at
					
					data['token_expiry'] = (datetime.now() + timedelta(seconds=exp)).isoformat()
			
					f.close()
					
					f = open(STORE, "w")
					f.write(json.dumps(data))
					f.close()
					
					print(data['access_token'])
					sys.exit(0)
				else:
					creddata['users'].remove(data)
	
		f.close()
		
	o_stdout, o_file = disable_stout()
	
	flow = OAuth2WebServerFlow(client_id=CLIENT_ID, client_secret=CLIENT_SECRET, scope=SCOPE, redirect_uri=REDIRECT_URL)
	
	#ensure cred files exist
	if not exists(STORE):
		f = open(STORE, 'w')
		f.write('{}')
		f.close()

	with open(NEWSTORE, 'a'):
		pass

	
	storage = Storage(NEWSTORE)
	credentials = run_flow(flow, storage)
	enable_stout(o_stdout, o_file)
	
#	print ("access_token: %s" % (credentials.access_token))
	print(credentials.access_token)
	
	s = open(STORE, "r")
	ns = open(NEWSTORE, "r")
	
	data = json.load(s)
	data = convertIfNeeded(data)
	newData = json.load(ns)
	
	data['users'].append({"username": username, "data": newData})
	
	ns.close()
	s.close()
	
	os.remove(NEWSTORE)
	
	f = open(STORE, "w")
	f.write(json.dumps(data))
	f.close()
	

getToken()