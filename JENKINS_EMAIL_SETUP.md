# Jenkins Email Notification Setup Guide

This guide will help you configure email notifications for your Jenkins pipeline.

## 📧 Overview

The Jenkins pipeline is now configured to send email notifications for:
- ✅ **SUCCESS** - When the build completes successfully
- ❌ **FAILURE** - When the build fails
- ⚠️ **UNSTABLE** - When the build has warnings or test failures

## 🔧 Jenkins Configuration

### Step 1: Install Required Plugin

1. Go to Jenkins Dashboard → **Manage Jenkins** → **Manage Plugins**
2. Click on the **Available** tab
3. Search for "**Email Extension Plugin**" (Email-ext)
4. Check the box and click **Install without restart**

### Step 2: Configure SMTP Server

1. Go to **Manage Jenkins** → **Configure System**
2. Scroll down to **Extended E-mail Notification** section
3. Configure the following:

#### For Gmail (Recommended for Testing)

```
SMTP server: smtp.gmail.com
SMTP port: 465
```

**Advanced Settings:**
- ✅ Check "Use SSL"
- Add credentials:
  - Username: your-email@gmail.com
  - Password: [App Password - see below]

#### Gmail App Password Setup

1. Go to your Google Account: https://myaccount.google.com/
2. Navigate to **Security**
3. Enable **2-Step Verification** (if not already enabled)
4. Go to **App passwords**
5. Generate a new app password for "Jenkins"
6. Use this 16-character password in Jenkins credentials

#### For Other Email Providers

##### Outlook/Office 365
```
SMTP server: smtp.office365.com
SMTP port: 587
Use TLS: Yes
```

##### Yahoo Mail
```
SMTP server: smtp.mail.yahoo.com
SMTP port: 465 or 587
Use SSL: Yes
```

##### Custom SMTP Server
```
SMTP server: your-smtp-server.com
SMTP port: 465 (SSL) or 587 (TLS)
```

### Step 3: Configure Default Recipients

In the **Extended E-mail Notification** section:

1. **Default Recipients**: Enter email addresses (comma-separated)
   ```
   your-email@example.com, team@example.com
   ```

2. **Default Subject**: (Optional - we use custom subjects in the pipeline)
   ```
   Jenkins Build: $PROJECT_NAME - $BUILD_STATUS
   ```

3. **Default Content**: (Optional - we use custom HTML templates)

4. **Default Triggers**: Check the boxes for:
   - ✅ Success
   - ✅ Failure
   - ✅ Unstable

### Step 4: Test Configuration

1. Scroll down and click **"Test configuration by sending test e-mail"**
2. Enter a test recipient email
3. Click **"Test configuration"**
4. Check your inbox for the test email

### Step 5: Configure Email Ext Plugin (Optional Advanced Settings)

Scroll to **E-mail Notification** section (standard Jenkins email):

1. **SMTP server**: smtp.gmail.com
2. **Advanced** → Configure same as Extended Email
3. **Test configuration** to verify

## 🎨 Email Templates

The pipeline includes three beautifully designed HTML email templates:

### Success Email Features:
- ✅ Green themed design
- Build information table
- Application access links
- "View Build Details" button
- Professional branding

### Failure Email Features:
- ❌ Red themed design
- Error information
- Troubleshooting steps
- "View Console Output" button
- Attached build log

### Unstable Email Features:
- ⚠️ Orange themed design
- Warning information
- Build details
- Attached build log

## 📝 Environment Variables

The email templates use these Jenkins environment variables:
- `${env.JOB_NAME}` - Project name
- `${env.BUILD_NUMBER}` - Build number
- `${env.BUILD_URL}` - Link to build
- `${env.GIT_COMMIT}` - Git commit hash
- `${currentBuild.durationString}` - Build duration
- `${DEFAULT_RECIPIENTS}` - Configured email recipients

## 🔐 Security Best Practices

1. **Never commit SMTP passwords** to version control
2. Use **App Passwords** instead of your main password (Gmail)
3. Store credentials in **Jenkins Credentials Manager**
4. Use **environment variables** for sensitive data
5. Restrict email recipients to team members only

## 🧪 Testing the Email Notifications

### Test Success Email
```bash
# Trigger a successful build
git add .
git commit -m "test: trigger successful build"
git push origin main
```

### Test Failure Email
```bash
# Temporarily break a test or build
# Push the change
# Fix it and push again
```

## 📧 Sample Email Recipients Configuration

### Single Recipient
```groovy
to: 'developer@example.com'
```

### Multiple Recipients
```groovy
to: 'developer@example.com, manager@example.com, team@example.com'
```

### Using Jenkins Credentials
```groovy
to: '${DEFAULT_RECIPIENTS}'  // Uses configured default recipients
```

### Dynamic Recipients
```groovy
to: "${env.CHANGE_AUTHOR_EMAIL}"  // For pull requests
```

## 🎯 Customization Options

### Change Email Subject
Modify the `subject` parameter in the Jenkinsfile:
```groovy
subject: "Custom Subject: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}"
```

### Attach Build Log
Set `attachLog: true` to include console output:
```groovy
attachLog: true  // Already enabled for failure and unstable
```

### Change MIME Type
Use plain text instead of HTML:
```groovy
mimeType: 'text/plain'
```

### Add Attachments
```groovy
attachmentsPattern: '**/target/*.jar'  // Attach build artifacts
```

## 🔍 Troubleshooting

### Issue: Emails Not Sending

**Check:**
1. SMTP configuration is correct
2. Credentials are valid
3. Jenkins has network access to SMTP server
4. Port is not blocked by firewall
5. Check Jenkins system log for errors

### Issue: Gmail Authentication Failed

**Solution:**
1. Enable 2-Step Verification
2. Generate App Password
3. Use App Password instead of regular password
4. Allow "Less secure app access" (not recommended)

### Issue: Emails Going to Spam

**Solution:**
1. Add Jenkins email to your contacts
2. Create email filter rule
3. Configure SPF/DKIM on your domain
4. Use professional email service

### Issue: HTML Not Rendering

**Solution:**
1. Check email client supports HTML
2. Verify `mimeType: 'text/html'` is set
3. Try viewing in different email client
4. Check email content security settings

## 📚 Additional Resources

- [Jenkins Email Extension Plugin Documentation](https://plugins.jenkins.io/email-ext/)
- [Jenkins Email Notification Guide](https://www.jenkins.io/doc/book/using/email/)
- [Gmail App Passwords](https://support.google.com/accounts/answer/185833)

## 💡 Tips

1. **Test in Development First**: Set up email notifications in a test Jenkins instance
2. **Use Distribution Lists**: Create team email lists instead of individual emails
3. **Monitor Email Quotas**: Be aware of SMTP server sending limits
4. **Archive Notifications**: Keep a record of build notifications for audit
5. **Customize Templates**: Adjust HTML templates to match your brand

## 🎉 Success!

Once configured, you'll receive professional email notifications for every build:
- Know immediately when builds fail
- Track build history via email
- Share build status with team automatically
- Get troubleshooting information instantly

---

**Questions?** Check the Jenkins system log at:
`Manage Jenkins → System Log → All Jenkins Logs`
