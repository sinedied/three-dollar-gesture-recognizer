##############################################################
#
# GestureRec3D.py -- 1$ Gesture Recognizer in 3D
#
# (c) Sven Kratz, Deutsche Telekom Labs, TU Berlin, 2008
#
# Implements the 1$ Gesture Recongizer with modification for
# 3D Data from Wii-Controller
# Modified GSS Search
# Custom Scoring Heuristic
#
# +++ PyS60 Version +++
#
###############################################################

from math import *
import traceback    # exceptions & co
from gesturelibrary import GestureLibrary
import operator # for fancy list sorting
#import cProfile # profiler
#import pstats

#from collections import deque

import time


RESAMPLE_POINTS = 250   # resample to n point gestures

VERBOSE_ = False

def saveTuplesToFile(file_name=None, tuples=[], file_handle = None, close = True):
    pass
    # Save Captured Acceleration Data to CSV File
    #print "TUPLES",tuples
    #try:
     #   if file_handle == None:
     #       file_handle = open(file_name, "w")
     #   for t in tuples:
     #       for i in t:
     #           outstr = str(i) + ","
    #            file_handle.write(outstr)
     #       file_handle.write('\n')
     #   if close:
     #       file_handle.close()
    #except Exception, e:
     #   print "Problem writing to file"
     #   print e
     #   print traceback.print_exc()
class GestureRec3D:
    def __init__(self, gLibrary = "./gestures.dat", resample_size = 50, gesture_recognized_callback = None):
        self.path_length = 0
        self.resample_amount  = resample_size
        self.gesture_path = []
        self.zero = (0,0,0)
        self.raw_data = None            # accelration deltas
        self.gesture_path = None        # path representation of raw data
        self.resampled_gesture = None   # path resampled to n equidistant points
        self.rotated_gesture = None     # path rotated to same angle as centroid
        self.normalized_gesture = None  # path normalized to fit in cube
        self.debug__ = False
        self.bbox_size = 100.0
        if gLibrary != None:
            self.gesture_lib = GestureLibrary(gLibrary)

        # gesture recognition heurstic paramters
        self.DETECTION_THRESHOLD = 0.35 # minimum score to consider gesture for detection
        self.INVALID_GESTURE = "nicht erkannt!"    # code for "no gesture detected"

        # for control by gesture application
        self.RECOGNIZE_GESTURES = True
        
        # callback in case of successful gesture recognition
        self.gesture_recognized_callback = gesture_recognized_callback
        

    def reset(self):
        """called after termination of matching operation"""
        self.path_length = 0
        #self.resample_amount  = 250
        self.gesture_path = []
        self.zero = (0,0,0)
        self.raw_data = None
        self.gesture_path = None
        self.resampled_gesture = None
        self.rotated_gesture = None
        self.normalized_gesture = None
        self.debug__ = False
        self.bbox_size = 100.0

    def deg_to_rad(self, angle):
        """convert degrees to radians"""
        return 2*pi * angle / 360

    def rad_to_deg(self,angle):
        """convert radians to degrees"""
        return (angle*360)/(2*pi)
    
    def save_data_to_library(self,gestureCode,sampleCounts=[25,50,100,150,250,300,500], gesture_list = None):
        """process current sample for use in gesture library"""
        currentSampleCount = self.resample_amount
        for l in sampleCounts:
            print "Resampling and saving to gesture length:",str(l)
            
            if gesture_list == None:
                gList = self.raw_data
            else:
                gList = gesture_list
            # path
            path = self.create_path(gList)
            # evenly resample path points
            resampled_gesture = self.resample_points(path, l)
            # rotate first of point to same angle as centroid
            ############################# !!!! Took out rotation
            rotated_gesture =  self.rotate_to_zero(resampled_gesture)
            #rotated_gesture = resampled_gesture
            # normalize gesture to fit in cube
            normalized_gesture = self.scale_to_cube(rotated_gesture, self.bbox_size)
            self.gesture_lib.add_gesture(gestureCode, normalized_gesture, save=False, gesture_length = l)
        #print ">>> Saving Gesture Lib..."
        #self.gesture_lib.save()
        #print ">>> ..done"
        self.gesture_lib.print_stats()
        
    def recognition_test(self, candidate_sets={}, training_sets={}, resample_size = 150):
        """ runs a recognition test 
            @param candidate_sets: the candidate sets
            @param training_sets: the training sets
            @param resample_size: the desired resampling size"""
        self.reset()
        self.resample_amount = resample_size
        results = []
        for Cgid,Cdict in candidate_sets.iteritems():
            Cgestures = Cdict[self.resample_amount]
            for Cg in Cgestures:
                rec_id,scoretable = self.recognize_pickled_gesture(Cg,training_sets)
                results.append([Cgid.strip(), rec_id, scoretable[:3]])
        
        print "======================="
        print "Recognition Results"
        count = 0;
        for item in results:
            print count,":", item[0],item[1],item[2]
            count += 1
        return results
                
                
    def recognize_pickled_gesture(self,candidate, training_sets, QUIET_=True):
        """ does a recognition of a finished candidate with training sets
            @param  candidate: a candidate gesture 
            @param  training sets: training set gesture library"""
        self.reset()
        print "###### Gesture recognition ####"
        summary = ""
        scoretable = []
        idnr = 0
        
        gesturelists = []
        for gid,dict in training_sets.iteritems():
            glist = dict[self.resample_amount]
            gesturelists.append([gid,glist])
        
        for gid, gesturelist in gesturelists:
            for gesture_trace in gesturelist:
                distance = self.distance_at_best_angle(pi, pi, pi, self.deg_to_rad(36), candidate, gesture_trace)
                score = self.score(distance)
                summary = summary + "GID "+ str(gid)+ " Idnr "+str(idnr)+" Dist "+str(distance)+" Score "+str(score) + "\n"
                scoretable.append([gid.strip(),idnr,distance,score])
                idnr = idnr + 1

            idnr = 0
        if QUIET_:
            print "==== Summary of Gesture Recognition ===="
            print "Sorted by highest score"
        # not python 2.2 compatible
        #scoretable.sort(key=operator.itemgetter(2));
        # fix:
        scoretable.sort(lambda x,y: cmp(x[2], y[2]))
        if QUIET_:
            for s in scoretable:
                print s
        rec_gest = self.recognize_from_scoreTable(scoretable)
        #print "====================================================="
        print ">>>>> Recognized Gesture id: ", rec_gest
        #print "====================================================="
        return (rec_gest,scoretable)
                
        
      
        


    def recognize_gesture(self, gList, rotated = True, callback=None):
        self.reset()

        self.raw_data = gList
        saveTuplesToFile("../debug/raw_samples.csv",gList)
        #self.path_length = self.calculate_path_length(gList)
        # create path from acceleration deltas
        self.gesture_path = self.create_path(gList)
        saveTuplesToFile("../debug/raw_gesture_path.csv",self.gesture_path)

        # evenly resample path points
        self.resampled_gesture = self.resample_points(self.gesture_path, self.resample_amount)
        saveTuplesToFile("../debug/resampled_path.csv",self.resampled_gesture)
        # rotate first point of gesture to same angle as centroid
        if rotated:
            self.rotated_gesture =  self.rotate_to_zero(self.resampled_gesture)
        else:
            self.rotated_gesture = self.resampled_gesture
        saveTuplesToFile("../debug/rotated_path.csv",self.rotated_gesture)
        # normalize gesture to fit in cube
        self.normalized_gesture = self.scale_to_cube(self.rotated_gesture, self.bbox_size)
        saveTuplesToFile("../debug/normalized_path.csv",self.normalized_gesture)

        # only try recognition if gesture library contains some gestures
        if (self.gesture_lib.has_gestures() and self.RECOGNIZE_GESTURES):
            print "###### Gesture recognition ####"
            summary = ""
            scoretable = []
            idnr = 0
            for gid, gesturelist in self.gesture_lib.get_gesture_lists(self.resample_amount):
                for gesture_trace in gesturelist:
                    # way too inefficient !?
                    #distance = self.distance_at_best_angle(pi, pi, pi, self.deg_to_rad(36), self.normalized_gesture, gesture_trace)
                    distance = self.path_distance(self.normalized_gesture, gesture_trace)
                    score = self.score(distance)
                    summary = summary + "GID "+ str(gid)+ " Idnr "+str(idnr)+" Dist "+str(distance)+" Score "+str(score) + "\n"
                    scoretable.append([gid.strip(),idnr,distance,score])
                    idnr = idnr + 1

                idnr = 0
            print "==== Summary of Gesture Recognition ===="
            print "Sorted by highest score"
            #scoretable.sort(key=operator.itemgetter(2));
            # not python 2.2 compatible
            #scoretable.sort(key=operator.itemgetter(2));
            # fix:
            scoretable.sort(lambda x,y: cmp(x[2], y[2]))
            for s in scoretable:
                print s
            rec_gest = self.recognize_from_scoreTable(scoretable)
            print "====================================================="
            print ">>>>> Recognized Gesture id: ", rec_gest
            print "====================================================="
            if self.gesture_recognized_callback != None:
                self.gesture_recognized_callback(rec_gest)
        else:
            print "...SKIPPING RECOGNITION"


    def recognize_from_scoreTable(self,scoretable):
        """Implements heuristic for gesture detection:
        int the top threee, detect at least two candidates of the same gesture id with a score >.55
        @param scoretable: scoretable sorted by score
        @return: recognized gesture code or invalid (-100) Gesture"""
        count_h1 = 0    # heuristic 1
        count_h2 = 0    # heuristic 2
        for item in scoretable[:3]:
            if item[3] > self.DETECTION_THRESHOLD*1.1: # if item 10% above threshold return directly
                return item[0]
            scoretable_cpy = scoretable[:]
            scoretable_cpy.remove(item)
            if item[3] >= self.DETECTION_THRESHOLD:
                print scoretable_cpy[:3]
                for other_item in scoretable_cpy[:2]:
                    print "Item:", [item[0],item[3]],"Cmp:", [other_item[0], other_item[3]]
                    if item[0] == other_item[0] and other_item[3] >= (self.DETECTION_THRESHOLD*0.95):
                        print "h1+",
                        count_h1 += 1
                    if item[0] == other_item[0]:
                        print "h2+",
                        count_h2 +=1
            if count_h1 >0:
                print "H_1"
                return item[0]
            elif count_h2 > 1:
                print "H_2"
                return item[0]
            else:
                count_h1 = 0
                count_h2 = 0
        return self.INVALID_GESTURE

    def read_from_csv(self, filename):
        f = open(filename,'r')
        lines = f.read().split("\n")
        glist = []
        for item in lines:
            vect = item.split(",")
            #print vect
            if len(vect) > 1:
                glist.append((int(vect[0]),int(vect[1]),int(vect[2])))
        return glist

    def save_current_gesture(self, gestureCode):
        """saves current processed gesture to gesture dictionary"""
        if self.normalized_gesture != None:
            self.gesture_lib.add_gesture(gestureCode, self.normalized_gesture)
        else:
            print "No gesture data available!"

    def create_path(self, gList):
        """creates a path from gList"""
        path = []
        for item in gList:
            if len(path) == 0:
                path.append(item)
            else:
                last = path[len(path)-1]
                newitem = (last[0]+item[0],last[1]+item[1],last[2]+item[2])
                path.append(newitem)
        return path

    def score(self, distance):
        """return normalized score for distances"""
        b = self.bbox_size
        return 1 - distance / (0.5* sqrt(b*b + b*b + b*b ))

    def path_distance(self,path1,path2):
        """compares two paths and returns the nornalized distance"""
        length1 = len(path1)
        length2 = len(path2)
        get_distance = self.distance
        distance = 0.0
        if length1 == length2:
            for i in range(length1):
                v1 = path1[i]
                v2 = path2[i]
                distance += get_distance(v1,v2)
            return distance / length1
        else:
            print "Warning: Path distances not equal:", length1,length2
            # work on sliced path
            if length1 < length2:
                return self.path_distance(path1,path2[:length1])
            else:
                return self.path_distance(path1[:length2],path2)

    def distance_at_angles(self,candidate, template, alpha, beta, gamma):
        """computes distance between candidate and template path at given angles alpha beta gamma"""

        matrix = self.rotationMatrixWithAngles3(alpha,beta,gamma)
        f_rotate = self.rotate3 # (p, matrix)
        newCandPoints = []
        #for p in candidate:
        #    newCandPoints.append(f_rotate(p,matrix))

        newCandPoints = [f_rotate(p,matrix) for p in candidate]

        dist = self.path_distance(newCandPoints, template)
        return dist

    def search_around_angle(self, candidate, template,angle, best_angles=[0.0,0.0,0.0]):
        """searches for minium distance around best_angles, using angle as offset
        @param candidate: the candidate points
        @param template: the template points
        @param best_angles: the angles where the last minimum distance was detected
        @param angle: angle to be checked around for improvement (add angle to best_angles)
        @return: minDist, newAngles (tuple containing the minimum distance and the best new angles"""

        # check all possible combinations of rotating the candidate points by angle
        #print "==== Input Angle", angle
        minDist = 2.0e50 # minium distance, initialize to large value
        minAngles = [0.0,0.0,0.0]
        for i in range(8):
            #add = best_angles is pointer assignment, so we have to copy the values!
            add = [best_angles[0],best_angles[1],best_angles[2]]
            #add = best_angles
            if i % 2 == 1:
                add[2]+=angle
            if i%4>1:
                add[1]+=angle
            if i%8>3:
                add[0]+=angle
            dist = self.distance_at_angles(candidate, template, add[0], add[1], add[2])

            if (dist < minDist):
                minDist = dist
                minAngles = [add[0],add[1],add[2]]
        if VERBOSE_:
            print "MinDist: ", minDist,"MinAngle:",minAngles
        return minDist, minAngles
        #return minDist

    
    






    def distance_at_best_angle(self,angularRangeX, angularRangeY, angularRangeZ, increment, candidate_points, library_points, cutoff_angle=2*pi*(15.0/360) ):
        """ @return: compares distance to candidate_points with points in library at various angles arouny x,y,z axis
            @param angularRange{X,Y,Z}: the search range (positive to negative that should be used)
            @param increment: search increment (in radians)
            @param candidate_points: the candidate point list (resampled, rotated, normalized)
            @param library_points: points from gesture library with (resampled, normalized, rotated, normalized) gestures
            @param cutoff_angle: angle at which Golden Section Search (GSS) is cut off default is 2 degrees
            """

        #print "ARXYZ", angularRangeX, angularRangeY, angularRangeZ,"Increment",increment

        # resolve function names -> slight optimization
        f_getMatrix = self.rotationMatrixWithAngles3
        f_path_distance = self.path_distance
        f_rotate3 = self.rotate3

        mind  = 2.0e50 # max float is what ?? >> todo: check IEE.xxx standard for fp-numbers
        maxd = -mind
        minDistAngle = 0.0
        maxDistAngle = 0.0

        # make lengths the same (still some bug in  equidistant point algorithm)
        # ugly solution: slice off extra points

        length1  = len(candidate_points)
        length2  = len(library_points)
        if length1 < length2:
                library_points = library_points[:length1]
        else:
                candidate_points  = candidate_points[:length2]
        length1  = len(candidate_points)
        length2  = len(library_points)

        #candidate_original = candidate_points

        print "Lengths",length1,length2



        ### Golden-Section Search ###

        theta_a = -angularRangeX # ignore other angular ranges for now
        theta_b = -theta_a
        theta_delta= cutoff_angle # angle at which GSS cuts off

        # best angles for lower / upper bound
        bestAngleLower = [0.0,0.0,0.0]
        bestAngleUpper = [0.0,0.0,0.0]

        best_angle = [0.0,0.0,0.0]

        # minimum distances
        # initialize minium lower and upper distances to high values
        minDistL = 2.0e50
        minDistU = 2.0e50

        phi  = 0.5*(-1+sqrt(5)) # golden section

        li = phi*theta_a+(1-phi)*theta_b # initial lower search angle

        minDistL,bestAngleLower = self.search_around_angle(candidate_points, library_points, li)

        ui = (1-phi)*theta_a + phi*theta_b # intial upper search angle

        minDistU,bestAngleUpper = self.search_around_angle(candidate_points, library_points,  ui)

        print "Best Angles", bestAngleLower, bestAngleUpper

        while abs(theta_b-theta_a) > theta_delta:
        #while self.distance(bestAngleLower,bestAngleUpper) > theta_delta:
            #print "Ta",theta_a,"Tb",theta_b,"Dif",self.distance(bestAngleLower,bestAngleUpper), "Delta",theta_delta

            if minDistL < minDistU:
                theta_b = ui
                ui = li
                minDistU = minDistL
                li = phi*theta_a+(1-phi)*theta_b
                minDistL,bestAngleLower = self.search_around_angle(candidate_points, library_points,  li)
            else:
                theta_a = li
                li = ui
                minDistL = minDistU
                ui = (1-phi)*theta_a + phi*theta_b
                minDistU,bestAngleUpper = self.search_around_angle(candidate_points, library_points, ui)

        print "GSS Results",minDistU,minDistL,"Best Angles",bestAngleUpper,bestAngleLower

        if minDistU >= minDistL:
            print "Returning",minDistL
            return minDistL
        else:
            print "Returning",minDistU
            return minDistU




        #searchUpper = [ui,ui,ui]   # upper bound of search

        # now search for best distance / angle
        # we have 2^3 = 8 different search directions for lower / upper
        #









        # todo: convert this to golden section search (GSS), at the moment too inefficient
        # right now, we are using brute force




        #return min

    def brute_force_recognition(self,angularRangeX, angularRangeY, angularRangeZ, candidate_points, library_points, increment=2.0*pi*(1.0/360), aFilename="../data/angle-dist.csv"):
        """Brute force version of gesture recognition algo. Searches all angles for minimum distance
            used to measure distance distribution by ange in MatLab"""
        # make lengths the same (still some bug in  equidistant point algorithm)
        # ugly solution: slice off extra points

        length1  = len(candidate_points)
        length2  = len(library_points)
        if length1 < length2:
                library_points = library_points[:length1]
        else:
                candidate_points  = candidate_points[:length2]
        length1  = len(candidate_points)
        length2  = len(library_points)
        #print "ARXYZ", angularRangeX, angularRangeY, angularRangeZ,"Increment",increment

        # resolve function names -> slight optimization
        f_getMatrix = self.rotationMatrixWithAngles3
        f_path_distance = self.path_distance
        f_rotate3 = self.rotate3

        #alpha = -angularRangeX
        #beta = -angularRangeY
        #gamma = -angularRangeZ


        distance_distrib = []


        #############
        # Brute-Force Approach
        #
        alpha = -angularRangeX
        beta = -angularRangeY
        gamma = -angularRangeZ
        mind = 2.0e10
        maxd = -mind
        distance_distrib = []



        tm = time.strftime("%d%m%y-%H%M%S")
        print tm
        ts = time.time()
        file_name = "./data/"+ aFilename + tm+".txt"

        try:
            resultfile_name = "./data/results.txt"
            f = open(resultfile_name,'a')
        except:
            print "problem with resultfile"
            pass

        print "Starting Calculation:", str(ts)
        #print "Saving Data To:",file_name
        #handle = open(file_name,'w')

        while alpha <= angularRangeX:
            while beta <= angularRangeY:
                while gamma <= angularRangeZ:

                    matrix = f_getMatrix(alpha,beta,gamma)
                    #newCandPoints = []
                    # rotate candidate points to new position: List Comprehension
                    newCandPoints = [f_rotate3(p,matrix) for p in candidate_points]
                    dist = f_path_distance(newCandPoints, library_points)
                    #print "Angles", alpha,beta,gamma, "Dist",dist
                    #data = [self.rad_to_deg(alpha),
                    #                         self.rad_to_deg(beta),
                    #                         self.rad_to_deg(gamma),
                    #                         dist]
                    #distance_distrib.append(data)
                    if dist < mind:
                        mind = dist
                        minDistAngle = [alpha,beta,gamma]
                    if dist > maxd:
                        maxd= dist
                        maxDistAngle = [alpha,beta,gamma]
                    gamma = gamma + increment

                beta = beta + increment
                gamma = -angularRangeZ
                #saveTuplesToFile(file_name=None,file_handle=handle,tuples=distance_distrib,close=False)
                #distance_distrib = []
            print mind,minDistAngle,maxd,maxDistAngle
            alpha = alpha + increment
            gamma = -angularRangeZ
            beta = -angularRangeY

        td = time.time()
        print "Ended calculation after:", str(ts-tm),"seconds."


        print "Min Distance Found:",mind, "Angle", minDistAngle, self.rad_to_deg(minDistAngle[0]),self.rad_to_deg(minDistAngle[1]),self.rad_to_deg(minDistAngle[2])
        print "Max Distance Found:", maxd, "Angle", maxdistAngle, self.rad_to_deg(maxDistAngle[0]), self.rad_to_deg(maxDistAngle[1]),self.rad_to_deg(maxDistAngle[2])
        #saveTuplesToFile(filename, distance_distrib)

        outstr = str([[mind, self.rad_to_deg(minDistAngle[0]),self.rad_to_deg(minDistAngle[1]),self.rad_to_deg(minDistAngle[2])],[maxd,self.rad_to_deg(maxDistAngle[0]), self.rad_to_deg(maxDistAngle[1]),self.rad_to_deg(maxDistAngle[2])]]) + "\n"

        try:
            f.write(outstr)
            f.close()
        except:
            pass
       # handle.close()




    def unit_vector(self,v):
        norm = 1.0 / self.distance_sqrt(v,(0,0,0))
        return (norm*v[0],norm*v[1],norm*v[2])

    def norm(self,u):
        """ returns norm of vector"""
        return sqrt(u[0]*u[0] + u[1]*u[1] + u[2]*u[2])

    def distance_sqrt(self,u,v):
        return sqrt((u[0]-v[0])*(u[0]-v[0])+(u[1]-v[1])*(u[1]-v[1])+(u[2]-v[2])*(u[2]-v[2]))

    def distance(self,u,v):
        """distance between tuple u and tuple v"""
        return sqrt((u[0]-v[0])*(u[0]-v[0])+(u[1]-v[1])*(u[1]-v[1])+(u[2]-v[2])*(u[2]-v[2]))
        #return (u[0]-v[0])*(u[0]-v[0])+(u[1]-v[1])*(u[1]-v[1])+(u[2]-v[2])*(u[2]-v[2])

    def calculate_path_length(self, gList):
        distance = 0.0
        index = 1
        while index < len(gList):
            p = gList[index]
            pl = gList[index-1]
            delta = self.distance(pl,p)
            distance = distance + delta
            index+=1


        return distance

    def centroid(self,points):
        """return centroid (i.e. mean x,y,z of point list)"""
        mx = 0.0
        my = 0.0
        mz = 0.0
        for p in points:
            mx = mx + p[0]
            my = my + p[1]
            mz = mz + p[2]
        return (mx / len(points),my / len(points), mz / len(points))

    def dot_product3(self,p,q):
        return p[0]*q[0]+p[1]*q[1]+p[2]*q[2]

    def norm_dot_product(self,u,v):
        """return normalized dot product (for angle calculation)"""
        return self.dot_product3(u, v)/ (self.norm(u)*self.norm(v))

    def angle3(self,u,v):
        """ returns the angle between vectors u and v"""
        #unitU = self.unit_vector(u)
        #unitV = self.unit_vector(v)

        norm_product = self.norm_dot_product(u, v)
        #print ">>>>>>>>>>>>The Norm Product", norm_product
        try:
            #print "####################"
            #print "Norm product",norm_product
            #print "###################"
            theta = acos(norm_product)
        except Exception, e:
            print e
            print traceback.print_exc()
            print "DOMAIN ERROR"
            print "========== Norms", self.norm(u), self.norm(v)
            print "=========== dot product", self.dot_product3(u, v)
            np = self.dot_product3(u, v) / (self.norm(u)* self.norm(v))
            print "Norm Product:", np, "ACos(1.0)", acos(1.0)
        return theta

    def orthogonal(self,b,c):
        """returns vector orthogonal to (cross-product of) b and c"""
        """a = b x c , mnemonic: xyzzy"""
        ax = b[1]*c[2] - b[2]*c[1] # ByCz - BzCy
        ay = b[2]*c[0] - b[0]*c[2] # BzCx - BxCz
        az = b[0]*c[1] - b[1]*c[0] # BxCy - ByCx
        return (ax,ay,az)

    def rotate_to_zero(self, points):
        """ rotate to zero using angle between centroid and first point"""
        centroid = self.centroid(points)
        if VERBOSE_: print "Centroid", centroid
         #print norm_product
        #print "Theta", theta


        theta = self.angle3(centroid, points[0])
        axis = self.unit_vector(self.orthogonal(points[0], centroid))
        r_matrix = self.rotationMatrixWithVector3(axis, theta)


        if VERBOSE_:print "Theta",theta
        rotated_points = []
        for p in points:
            newpoint = self.rotate3(p, r_matrix)

            rotated_points.append(newpoint)
        #print "Rotated Points", rotated_points
        # debug: angle between first point and centroid should be zero now
        if VERBOSE_: print "========>centroid", centroid
        #nullangle = self.angle3(centroid, centroid)
        nullangle = self.norm_dot_product(centroid, centroid)
        angle = self.norm_dot_product(centroid, rotated_points[0])
        if VERBOSE_: print "Cosine Null Angle",nullangle,"Cosine Debug Angle:", angle, " >>1.0 is good!"
        return rotated_points

    def bounding_box3(self,points):
        """return bounding box in 3d space of set of points"""
        mmx = [points[0][0],points[0][0]]
        mmy = [points[0][1],points[0][1]]
        mmz = [points[0][2],points[0][2]]

        for p in points:
            if p[0] <= mmx[0]:
                mmx[0] = p[0]
            elif p[0] > mmx[1]:
                mmx[1] = p[0]

            if p[1] <= mmy[0]:
                mmy[0] = p[1]
            elif p[1] > mmy[1]:
                mmy[1] = p[1]

            if p[2] <= mmz[0]:
                mmz[0] = p[2]
            elif p[2] > mmz[1]:
                mmz[1] = p[2]
        bbox = [mmx,mmy,mmz]
        if VERBOSE_:print "Bounding Box:", bbox
        return bbox


    def rotationMatrixWithAngles3(self,a,b,g):
        rx = [cos(a)*cos(b), cos(a)*sin(b)*sin(g)-sin(a)*cos(g),cos(a)*sin(b)*cos(g)+sin(a)*sin(g)]
        ry = [sin(a)*cos(b), sin(a)*sin(b)*sin(g)+cos(a)*cos(g), sin(a)*sin(b)*cos(g) - cos(a)*sin(g)]
        rz = [-sin(b) , cos(b)*sin(g), cos(b)*cos(g)]
        #print "Rotation Matrix", [rx,ry,rz]
        return [rx,ry,rz]

    def rotationMatrixWithVector3(self,axis,angle):
        x = axis[0]
        y = axis[1]
        z = axis[2]
        rx = [1 + (1-cos(angle))*(x*x-1), -z*sin(angle)+(1-cos(angle))*x*y, y*sin(angle)+(1-cos(angle))*x*z]
        ry = [z*sin(angle)+(1-cos(angle))*x*y,  1 + (1-cos(angle))*(y*y-1),  -x*sin(angle)+(1-cos(angle))*y*z]
        rz = [-y*sin(angle)+(1-cos(angle))*x*z,    x*sin(angle)+(1-cos(angle))*y*z, 1 + (1-cos(angle))*(z*z-1) ]
        if VERBOSE_:print "Rotation Matrix", [rx,ry,rz]
        return [rx,ry,rz]

    def scale_to_cube(self,points,size):
        """scale set of points to a cube of standardized size"""
        bbox = self.bounding_box3(points)
        bwx = abs(bbox[0][0] - bbox[0][1])
        bwy = abs(bbox[1][0] - bbox[1][1])
        bwz = abs(bbox[2][0] - bbox[2][1])
        newpoints = []
        if VERBOSE_:print "BBox Widths:",bwx,bwy,bwz

        for p in points:
            qx = p[0] * (size / bwx)
            qy = p[1] * (size / bwy)
            qz = p[2] * (size / bwz)
            newpoints.append((qx,qy,qz))
        if self.debug__:
            bbox = self.bounding_box3(newpoints)
            bwx = abs(bbox[0][0] - bbox[0][1])
            bwy = abs(bbox[1][0] - bbox[1][1])
            bwz = abs(bbox[2][0] - bbox[2][1])
            if VERBOSE_:print "New BBox Widths:",bwx,bwy,bwz
        return newpoints

    def rotate3(self, p, matrix):
        """multiply 3x3 (rotation-matrix) with point, using list comprehension"""
        return [p[0]*x[0] + p[1]*x[1]+ p[2]*x[2] for x in matrix]

    #def rotate3(self, p, matrix):
    #    """multiply 3x3 (rotation-)matrix with point"""
    #    out = []
    #
    #    for r in matrix: # row in matrix
    #        out.append(r[0] * p[0] + r[1] * p[1] + r[2] * p[2])
    #    return (out[0],out[1],out[2])

    def resample_points(self,gList, numSamples):
        #print gList
        path_length = self.calculate_path_length(gList)
        increment = (path_length / float(numSamples+1))
        sum_distance  = delta = 0.0
        qx = qy = qz =0
        #last= (0,0,0)
        newpoints = []
        if VERBOSE_:
            print "NumSamples", numSamples
            print "Path Length", path_length
            print "Increment", increment

        ## Step-through algorithm ( from paper )
        last = (0,0,0)
        p = gList[0]
        index = 1
       # p = gList[index]
        #pl = gList[index-1]
        count = 1
        path = []
        path.append(gList[index])
        index += 1
        while index < len(gList)-1:
            length = self.calculate_path_length(path)
            #print length
            #if length <= increment:
            if length < increment:
                index= index + 1
                path.append(gList[index])
            else:
               # calculate unit vector from last two vectors in path
               v1 = path[len(path)-1]
               v2 = path[len(path)-2]
               diff = (v1[0] - v2[0],v1[1] - v2[1],v1[2] - v2[2])
               unitV = self.unit_vector(diff)
               #print path
               #print count, length,
               path.remove(v1)
               missing_incr = length - increment
               newpoint = (v1[0] - missing_incr * unitV[0],
                           v1[1] - missing_incr * unitV[1],
                           v1[2] - missing_incr * unitV[2])
               #print missing_incr
               newpoints.append(newpoint)
               del path
               path = []
               #gList.insert(index+1,newpoint)
               path.append(newpoint)
               #index = index + 1
               #path.append(gList[index])
               path.append(v1)
               count = count + 1









        print "Points Resampled, length ", len(newpoints)

        #for i in range(1,len(newpoints)):
        #    print i," D:" , self.distance(newpoints[i],newpoints[i-1]), newpoints[i]

        return newpoints

#if __name__ == "__main__":
#    g = GestureRec3D()
#    glist = g.read_from_csv("../traces/280508-05211.txt")
    #g.recognize_gesture(glist)
#    g.recognize_gesture(glist)
    #cProfile.run('g.recognize_gesture(glist)', './profile-stats.dat')
#    p = pstats.Stats('./profile-stats.dat')
#    p.sort_stats('time').print_stats(10)


